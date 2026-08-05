package timeline;

import disassemblers.Z80Dis;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.CRC32;
import static timeline.MemoryRecord.nMemBaseAddress;

public class MemoryTimeline {

    private final int maxRecords; // Maximalni pocet zaznamu ve fronte
    private final LinkedList<MemoryRecord> timeline; // Fronta zaznamu jako propojeny seznam
    private final byte[] previousMemory; // Predchozi kompletni stav timeline
    private final long[] previousCRCs; // CRC hodnoty jednotlivych bloku pameti
    private final int blockSize = 256; // Velikost bloku pro vypocet CRC
    private final int numBlocks; // Pocet bloku pameti
    private int currentIndex; // Aktualni index ve fronte
    private int stepCount; // Pocet kroku od posledniho snapshotu
    private final int snapshotInterval; // Interval pro vytvoreni snapshotu
    private final boolean[] validBlocks; // Platnost jednotlivych bloku pro CRC
    private boolean isRemovingOldestBlock = false; // Indikuje probihajici odstranovani nejstarsiho bloku
    private final Object lock = new Object(); // Synchronizacni objekt pro pristup k timeline
    private final List<ChangeLog> prubezneZmeny; // Zapisy do RAM od posledniho update()

    public MemoryTimeline(int maxRecords, int memorySize, int snapshotInterval) {
        this.maxRecords = maxRecords;
        this.timeline = new LinkedList<MemoryRecord>();
        this.previousMemory = new byte[memorySize];
        this.previousCRCs = new long[memorySize / blockSize + 1];
        this.numBlocks = memorySize / blockSize;
        this.currentIndex = -1;
        this.stepCount = 0;
        this.snapshotInterval = snapshotInterval;
        this.validBlocks = new boolean[(memorySize + blockSize - 1) / blockSize];
        this.prubezneZmeny = new ArrayList<ChangeLog>();
        Arrays.fill(validBlocks, true);
    }

    // Zpracovani noveho stavu. Zaznam na indexu N predstavuje stav CPU
    // pred instrukci N. Zmeny ulozene v tomto zaznamu provedla instrukce N-1.
    public void update(byte[] currentMemory) {
        synchronized (lock) {
            if (currentMemory == null || currentMemory.length != previousMemory.length) {
                throw new IllegalArgumentException("Unexpected timeline snapshot size");
            }

            if (timeline.isEmpty()) {
                prubezneZmeny.clear();
                addRecord(new MemoryRecord(Arrays.copyOf(currentMemory, currentMemory.length),
                        new ArrayList<ChangeLog>()));
                System.arraycopy(currentMemory, 0, previousMemory, 0, currentMemory.length);
                return;
            }

            stepCount++;

            // Zmeny je nutne odebrat i pri pravidelnem snapshotu. Puvodni kod
            // je pri snapshotu nechal v prubezneZmeny a priradil je az dalsi instrukci.
            List<ChangeLog> changes = compareMemoryWithCRC(previousMemory, currentMemory);

            if (stepCount >= snapshotInterval) {
                addRecord(new MemoryRecord(Arrays.copyOf(currentMemory, currentMemory.length), changes));
                stepCount = 0;
            } else if (changes.size() > currentMemory.length / 2) {
                addRecord(new MemoryRecord(Arrays.copyOf(currentMemory, currentMemory.length), changes));
                stepCount = 0;
            } else {
                addRecord(new MemoryRecord(changes));
            }

            System.arraycopy(currentMemory, 0, previousMemory, 0, previousMemory.length);
        }
    }

    private void addRecord(MemoryRecord record) {
        synchronized (lock) {
            timeline.addLast(record);
            currentIndex = timeline.size() - 1;
        }

        if (timeline.size() > maxRecords) {
            removeOldestBlock();
        }
    }

    private void removeOldestBlock() {
        if (isRemovingOldestBlock) {
            return;
        }

        isRemovingOldestBlock = true;
        boolean snapshotFound = false;

        while (true) {
            synchronized (lock) {
                if (timeline.isEmpty()) {
                    break;
                }

                MemoryRecord record = timeline.getFirst();
                if (record.getType() == MemoryRecord.SNAPSHOT) {
                    if (snapshotFound) {
                        break;
                    }
                    snapshotFound = true;
                }
                timeline.removeFirst();
            }
        }

        isRemovingOldestBlock = false;
    }

    public void invalidateBlock(int address) {
        int blockIndex = (MemoryRecord.nMemBaseAddress + address) / blockSize;
        validBlocks[blockIndex] = false;
    }

    // poke8() vola addChange az po zapisu do emulovane pameti. Stary bajt
    // proto bereme z predchoziho timeline stavu. RAM zacina az na offsetu 42.
    public void addChange(int address, byte value) {
        synchronized (lock) {
            if (address < 0 || address >= 0x10000) {
                throw new IllegalArgumentException("Memory address outside Z80 address space: " + address);
            }

            int stateAddress = MemoryRecord.nMemBaseAddress + address;
            byte oldValue = previousMemory[stateAddress];

            // Pokud jedna instrukce zapise na stejnou adresu vicekrat, druhy zapis
            // musi navazovat na vysledek prvniho, ne znovu na stav pred instrukci.
            for (int i = prubezneZmeny.size() - 1; i >= 0; i--) {
                ChangeLog previousChange = prubezneZmeny.get(i);
                if (previousChange != null
                        && previousChange.address == stateAddress
                        && previousChange.newValues != null
                        && previousChange.newValues.length > 0) {
                    oldValue = previousChange.newValues[previousChange.newValues.length - 1];
                    break;
                }
            }

            prubezneZmeny.add(new ChangeLog(stateAddress, oldValue, value));
        }
    }

    private List<ChangeLog> compareMemoryWithCRC(byte[] previous, byte[] current) {
        List<ChangeLog> changes = new ArrayList<ChangeLog>();

        // Registry, stav CPU, porty a T-states.
        for (int i = 0; i < MemoryRecord.nMemBaseAddress; i++) {
            if (previous[i] != current[i]) {
                int start = i;
                while (i < MemoryRecord.nMemBaseAddress && previous[i] != current[i]) {
                    i++;
                }
                changes.add(new ChangeLog(start,
                        Arrays.copyOfRange(previous, start, i),
                        Arrays.copyOfRange(current, start, i)));
            }
        }

        // Skutecne zapisy do RAM zachycene v poke8().
        changes.addAll(prubezneZmeny);
        prubezneZmeny.clear();
        return changes;
    }

    public void dumpInfo(int index) {
        MemoryRecord record = timeline.get(index);
        if (record.getType() != MemoryRecord.SNAPSHOT) {
            record.dumpInfo(index);
        }
    }

    public byte[] getMemoryAt(int index) {
        synchronized (lock) {
            if (index < 0 || index >= timeline.size()) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index);
            }

            MemoryRecord targetRecord = timeline.get(index);
            if (targetRecord.getType() == MemoryRecord.SNAPSHOT) {
                return Arrays.copyOf(targetRecord.getMemory(), previousMemory.length);
            }

            ListIterator<MemoryRecord> iterator = timeline.listIterator(index);
            while (iterator.hasPrevious()) {
                MemoryRecord record = iterator.previous();
                if (record.getType() == MemoryRecord.SNAPSHOT) {
                    byte[] memory = Arrays.copyOf(record.getMemory(), previousMemory.length);

                    // listIterator(index) stoji pred zaznamem s indexem index.
                    // Aplikovat se musi i cilovy zaznam, protoze obsahuje stav
                    // pred prave zobrazovanou instrukci.
                    while (iterator.hasNext() && iterator.nextIndex() <= index) {
                        record = iterator.next();
                        if (record.getType() == MemoryRecord.STEP) {
                            for (ChangeLog change : record.getChanges()) {
                                System.arraycopy(change.newValues, 0, memory,
                                        change.address, change.newValues.length);
                            }
                        }
                    }

                    return memory;
                }
            }

            throw new IllegalStateException(
                    "No snapshot found in the timeline for index=" + index);
        }
    }

    public void truncateTimeline(int index) {
        synchronized (lock) {
            while (timeline.size() > index + 1) {
                MemoryRecord record = timeline.removeLast();
                record.clear();
            }

            currentIndex = Math.min(currentIndex, index);
        }
    }

    public void clearTimeline() {
        synchronized (lock) {
            for (MemoryRecord record : timeline) {
                record.clearAll();
            }

            timeline.clear();
            currentIndex = -1;
            stepCount = 0;
            isRemovingOldestBlock = false;
            prubezneZmeny.clear();

            Arrays.fill(previousMemory, (byte) 0);
            Arrays.fill(previousCRCs, 0L);
            Arrays.fill(validBlocks, true);
        }
    }

    /**
     * Exportuje timeline jako zarovnany ASM trace. Jeden dokonceny krok je
     * zapsan na jednom radku
     */
    public int exportToText(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Export file must not be null");
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file),
                        StandardCharsets.UTF_8), 64 * 1024)) {

            synchronized (lock) {
                if (timeline.size() < 2) {
                    return 0;
                }

                ListIterator<MemoryRecord> iterator = timeline.listIterator();
                MemoryRecord firstRecord = iterator.next();
                if (firstRecord == null
                        || firstRecord.getType() != MemoryRecord.SNAPSHOT
                        || firstRecord.getMemory() == null) {
                    throw new IOException("Timeline does not start with a valid snapshot");
                }

                byte[] state = Arrays.copyOf(firstRecord.getMemory(),
                        firstRecord.getMemory().length);
                validateStateSize(state, 0);

                // Ctyri bajty navic umozni disassembleru precist instrukci,
                // ktera zacina na konci adresniho prostoru a pokracuje od 0000h.
                int[] opcodes = new int[0x10000 + 4];
                copyRamToOpcodes(state, opcodes);

                int index = 0;
                int exported = 0;

                while (iterator.hasNext()) {
                    MemoryRecord nextRecord = iterator.next();
                    if (nextRecord == null) {
                        throw new IOException("Null timeline record at index " + (index + 1));
                    }

                    // Radek se sestavi ze stavu pred instrukci. Teprve potom
                    // aplikujeme nasledujici zaznam a doplnime zapisy do RAM.
                    StringBuilder line = buildTraceLine(state, opcodes);
                    List<MemoryWrite> writes = applyNextRecord(
                            state, opcodes, nextRecord, index + 1);
                    appendMemoryWrites(line, writes);

                    writer.write(line.toString());
                    writer.newLine();

                    index++;
                    exported++;
                }

                writer.flush();
                return exported;
            }
        }
    }

    private static StringBuilder buildTraceLine(byte[] state, int[] opcodes) {
        int pc = readWordLittleEndian(state, 25);
        int status = state[19] & 0xFF;
        int flags = state[21] & 0xFF;
        int length = 1;
        String mnemonic;

        synchronized (Z80Dis.class) {
            int[] oldOpcodes = Z80Dis.Opcodes;
            try {
                Z80Dis.Opcodes = opcodes;
                Z80Dis disassembler = new Z80Dis();
                length = disassembler.OpcodeLen(pc) & 0xFF;
                if (length < 1 || length > 4) {
                    length = 1;
                }

                mnemonic = disassembler.Disassemble(pc).trim();
                if (mnemonic.length() == 0) {
                    mnemonic = "DB #" + hexByte(opcodes[pc]);
                    length = 1;
                }
            } catch (RuntimeException ex) {
                mnemonic = "DB #" + hexByte(opcodes[pc]);
                length = 1;
            } finally {
                Z80Dis.Opcodes = oldOpcodes;
            }
        }

        StringBuilder bytes = new StringBuilder(11);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                bytes.append(' ');
            }
            appendHex(bytes, opcodes[pc + i], 2);
        }

        StringBuilder line = new StringBuilder(256);
        appendHex(line, pc, 4);
        line.append(": ");
        appendPadded(line, bytes.toString(), 12);
        appendPadded(line, mnemonic, 20);

        line.append("AF=");
        appendRegisterPair(line, state, 22, 21);
        line.append(" BC=");
        appendRegisterPair(line, state, 14, 13);
        line.append(" DE=");
        appendRegisterPair(line, state, 12, 11);
        line.append(" HL=");
        appendRegisterPair(line, state, 10, 9);

        line.append("  AF'=");
        appendRegisterPair(line, state, 8, 7);
        line.append(" BC'=");
        appendRegisterPair(line, state, 6, 5);
        line.append(" DE'=");
        appendRegisterPair(line, state, 4, 3);
        line.append(" HL'=");
        appendRegisterPair(line, state, 2, 1);

        line.append("  IX=");
        appendHex(line, readWordLittleEndian(state, 17), 4);
        line.append(" IY=");
        appendHex(line, readWordLittleEndian(state, 15), 4);
        line.append(" SP=");
        appendHex(line, readWordLittleEndian(state, 23), 4);
        line.append(" I=");
        appendHex(line, state[0] & 0xFF, 2);
        line.append(" R=");
        appendHex(line, state[20] & 0xFF, 2);
        line.append(" IM=").append(state[27] & 0xFF);
        line.append(" IFF=").append(status & 1).append((status >>> 1) & 1);
        line.append(' ');

        // HALT se ukaze jen pri aktivnim stavu, pole ale zustava pevne siroke.
        appendPadded(line, ((status & 0x20) != 0) ? "HALT" : "", 5);
        appendPadded(line, "F=" + formatFlags(flags), 18);
        line.append("T=").append(Long.toUnsignedString(
                readLongBigEndian(state, 34)));

        return line;
    }

    private static String formatFlags(int flags) {
        StringBuilder out = new StringBuilder(18);
        // S - sign: M = minus, P = plus
        out.append((flags & 0x80) != 0 ? "M" : "P");
        // Z - zero
        out.append(',').append((flags & 0x40) != 0 ? "Z" : "NZ");
        // H - half carry; vypisujeme pouze pokud je nastaven
        if ((flags & 0x10) != 0) {
            out.append(",H");
        }
        // P/V - parity/overflow
        out.append(',').append((flags & 0x04) != 0 ? "PE" : "PO");
        // N - subtract; vypisujeme pouze pokud je nastaven
        if ((flags & 0x02) != 0) {
            out.append(",N");
        }
        // C - carry
        out.append(',').append((flags & 0x01) != 0 ? "C" : "NC");
        return out.toString();
    }

    private static List<MemoryWrite> applyNextRecord(byte[] state,
            int[] opcodes, MemoryRecord record, int recordIndex) throws IOException {
        List<MemoryWrite> writes = new ArrayList<MemoryWrite>();
        List<ChangeLog> changes = record.getChanges();

        if (changes != null) {
            for (ChangeLog change : changes) {
                applyChange(state, opcodes, change, recordIndex, writes);
            }
        }

        if (record.getType() == MemoryRecord.SNAPSHOT) {
            byte[] snapshot = record.getMemory();
            validateStateSize(snapshot, recordIndex);
            System.arraycopy(snapshot, 0, state, 0, state.length);
            copyRamToOpcodes(state, opcodes);
        } else if (record.getType() != MemoryRecord.STEP) {
            throw new IOException("Unknown timeline record type at index " + recordIndex);
        }

        return writes;
    }

    private static void applyChange(byte[] state, int[] opcodes,
            ChangeLog change, int recordIndex, List<MemoryWrite> writes)
            throws IOException {
        if (change == null || change.newValues == null) {
            throw new IOException("Invalid change at timeline index " + recordIndex);
        }

        int start = change.address;
        int end = start + change.newValues.length;
        if (start < 0 || end < start || end > state.length) {
            throw new IOException("Change outside timeline state at index "
                    + recordIndex + ": address=" + start
                    + ", length=" + change.newValues.length);
        }

        for (int i = 0; i < change.newValues.length; i++) {
            int stateAddress = start + i;
            int oldValue;

            if (change.oldValues != null && i < change.oldValues.length) {
                oldValue = change.oldValues[i] & 0xFF;
            } else {
                oldValue = state[stateAddress] & 0xFF;
            }

            int newValue = change.newValues[i] & 0xFF;

            if (stateAddress >= nMemBaseAddress) {
                writes.add(new MemoryWrite(stateAddress - nMemBaseAddress,
                        oldValue, newValue));
            }

            state[stateAddress] = change.newValues[i];
            updateOpcodeByte(opcodes, stateAddress, newValue);
        }
    }

    private static void appendMemoryWrites(StringBuilder line,
            List<MemoryWrite> writes) {
        if (writes.isEmpty()) {
            return;
        }

        // Pametove zmeny zacinaji ve stejnem sloupci bez ohledu na delku T-states.
        while (line.length() < 190) {
            line.append(' ');
        }
        if (line.length() >= 190) {
            line.append(' ');
        }

        line.append("M[");
        for (int i = 0; i < writes.size(); i++) {
            if (i > 0) {
                line.append(',');
            }

            MemoryWrite write = writes.get(i);
            appendHex(line, write.address, 4);
            line.append(':');
            appendHex(line, write.oldValue, 2);
            line.append("->");
            appendHex(line, write.newValue, 2);
        }
        line.append(']');
    }

    private static void validateStateSize(byte[] state, int index)
            throws IOException {
        if (state == null || state.length < nMemBaseAddress + 0x10000) {
            throw new IOException("Incomplete timeline state at index " + index);
        }
    }

    private static void copyRamToOpcodes(byte[] state, int[] opcodes) {
        for (int address = 0; address < 0x10000; address++) {
            opcodes[address] = state[nMemBaseAddress + address] & 0xFF;
        }

        for (int i = 0; i < 4; i++) {
            opcodes[0x10000 + i] = opcodes[i];
        }
    }

    private static void updateOpcodeByte(int[] opcodes,
            int stateAddress, int newValue) {
        if (stateAddress < nMemBaseAddress) {
            return;
        }

        int address = stateAddress - nMemBaseAddress;
        opcodes[address] = newValue;

        if (address < 4) {
            opcodes[0x10000 + address] = newValue;
        }
    }

    private static void appendPadded(StringBuilder out, String value, int width) {
        out.append(value);
        for (int i = value.length(); i < width; i++) {
            out.append(' ');
        }
    }

    private static void appendRegisterPair(StringBuilder out, byte[] state,
            int highByteOffset, int lowByteOffset) {
        int value = ((state[highByteOffset] & 0xFF) << 8)
                | (state[lowByteOffset] & 0xFF);
        appendHex(out, value, 4);
    }

    private static int readWordLittleEndian(byte[] state, int offset) {
        return (state[offset] & 0xFF)
                | ((state[offset + 1] & 0xFF) << 8);
    }

    private static long readLongBigEndian(byte[] state, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (state[offset + i] & 0xFFL);
        }
        return value;
    }

    private static String hexByte(int value) {
        StringBuilder out = new StringBuilder(2);
        appendHex(out, value, 2);
        return out.toString();
    }

    private static void appendHex(StringBuilder out, int value, int digits) {
        final char[] hex = "0123456789ABCDEF".toCharArray();
        for (int shift = (digits - 1) * 4; shift >= 0; shift -= 4) {
            out.append(hex[(value >>> shift) & 0x0F]);
        }
    }

    private static class MemoryWrite {

        final int address;
        final int oldValue;
        final int newValue;

        MemoryWrite(int address, int oldValue, int newValue) {
            this.address = address;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    public int getTimelineSize() {
        synchronized (lock) {
            return timeline.size();
        }
    }
}

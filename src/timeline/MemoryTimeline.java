package timeline;

import java.util.*;
import java.util.zip.CRC32;
import static timeline.MemoryRecord.nMemBaseAddress;

public class MemoryTimeline {

    private final int maxRecords; // Maximální počet záznamů ve frontě
    private final LinkedList<MemoryRecord> timeline; // Fronta záznamů jako propojený seznam
    private final byte[] previousMemory; // Předchozí stav paměti
    private final long[] previousCRCs; // CRC hodnoty pro bloky
    private final int blockSize = 256; // Velikost bloku pro CRC
    private final int numBlocks; // Počet bloků paměti
    private int currentIndex; // Aktuální index ve frontě
    private int stepCount; // Počet kroků od posledního snapshotu
    private final int snapshotInterval; // Interval pro vytvoření snapshotu
    private final boolean[] validBlocks; // Pole pro validitu bloků
    private boolean isRemovingOldestBlock = false; // indikuje zda-li jiz odstranovani probliha    
    private final Object lock = new Object(); // Synchronizační objekt
    List<ChangeLog> prubezneZmeny = null;

    // Konstruktor
    public MemoryTimeline(int maxRecords, int memorySize, int snapshotInterval) {
        this.maxRecords = maxRecords;
        this.timeline = new LinkedList<MemoryRecord>();
        this.previousMemory = new byte[memorySize]; // Inicializováno na nuly
        this.previousCRCs = new long[memorySize / blockSize + 1]; // Inicializace CRC hodnot
        this.numBlocks = memorySize / blockSize;
        this.currentIndex = -1; // Index je na začátku neplatný
        this.stepCount = 0; // Počítadlo kroků od posledního snapshotu
        this.snapshotInterval = snapshotInterval; // Interval pro vytvoření snapshotu
        this.validBlocks = new boolean[(memorySize + blockSize - 1) / blockSize];
        prubezneZmeny = new ArrayList<ChangeLog>();
        Arrays.fill(validBlocks, true);

    }

    // Metoda pro zpracování nového stavu paměti
    public void update(byte[] currentMemory) {
        // Pokud je fronta prázdná (první volání), vytvoř snapshot na začátek
        if (timeline.isEmpty()) {
            addRecord(new MemoryRecord(Arrays.copyOf(currentMemory, currentMemory.length)));
            System.arraycopy(currentMemory, 0, previousMemory, 0, currentMemory.length); // Aktualizace paměti
            return; // Ukonči metodu, protože snapshot byl vytvořen
        }
        // Zvýšení počítadla kroků
        stepCount++;

        // Kontrola: vytvoř pravidelný snapshot, pokud je dosažen interval
        if (stepCount >= snapshotInterval) {
            addRecord(new MemoryRecord(Arrays.copyOf(currentMemory, currentMemory.length)));
            stepCount = 0; // Resetuje počítadlo kroků
            System.arraycopy(currentMemory, 0, previousMemory, 0, currentMemory.length); // Aktualizace paměti
            return;
        }

        // Porovnej aktuální a předchozí stav paměti
        List<ChangeLog> changes = compareMemoryWithCRC(previousMemory, currentMemory);

        // Pokud je více než polovina paměti změněna, vytvoř snapshot
        if (changes.size() > currentMemory.length / 2) {
            addRecord(new MemoryRecord(Arrays.copyOf(currentMemory, currentMemory.length)));
            stepCount = 0; // Resetuje počítadlo kroků
        } else {
            // Jinak ulož změny
            addRecord(new MemoryRecord(changes));
        }        
        //aktualni stav uloz jako predchozui pro pristi porovnani
        System.arraycopy(currentMemory, 0, previousMemory, 0, previousMemory.length);
    }

    // Přidání záznamu do fronty
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
            return; // Pokud už odstranění probíhá, ukonči metodu
        }
        isRemovingOldestBlock = true;
        boolean snapshotFound = false;
        while (true) {
            synchronized (lock) {
                if (!timeline.isEmpty()) {
                    MemoryRecord record = timeline.getFirst();

                    // Pokud narazíme na snapshot, zastav mazání
                    if (record.getType() == MemoryRecord.SNAPSHOT) {
                        if (snapshotFound) {
                            //pokud je to uz druhy snapshot, tak vyskoc
                            break;
                        }
                        snapshotFound = true; // Ujistíme se, že jeden snapshot zůstane
                    }
                    timeline.removeFirst();
                   // record.clear(); // Uvolnění paměti záznamu
                } else {
                    break;
                }
            }
        }
        isRemovingOldestBlock = false;

    }

    public void invalidateBlock(int address) {
        int blockIndex = (MemoryRecord.nMemBaseAddress+address) / blockSize;
        validBlocks[blockIndex] = false; // Označ blok jako neplatný         
    }

    public void addChange(int address, byte value) {
        prubezneZmeny.add(new ChangeLog(MemoryRecord.nMemBaseAddress+address, previousMemory[address], value));
    }

    private List<ChangeLog> compareMemoryWithCRC(byte[] previousMemory, byte[] currentMemory) {
        List<ChangeLog> changes = new ArrayList<ChangeLog>();
        for (int i = 0; i < MemoryRecord.nMemBaseAddress; i++) {
            if (previousMemory[i] != currentMemory[i]) {
                int start = i;
                while (i < MemoryRecord.nMemBaseAddress && previousMemory[i] != currentMemory[i]) {
                    i++;
                }
                changes.add(new ChangeLog(start,
                        Arrays.copyOfRange(previousMemory, start, i),
                        Arrays.copyOfRange(currentMemory, start, i)));
            }
        }
        changes.addAll(prubezneZmeny);
        prubezneZmeny.clear();
        return changes;
    }

   
    public void dumpInfo(int index) {
        MemoryRecord record=timeline.get(index);
        if(record.getType()!=MemoryRecord.SNAPSHOT){
            record.dumpInfo(index);
        }
    }
    
    public byte[] getMemoryAt(int index) {
        synchronized (lock) { // Synchronizace přístupu k timeline
            if (index < 0 || index >= timeline.size()) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index);
            }

            // Získání záznamu na daném indexu
            MemoryRecord targetRecord = timeline.get(index);

            // Pokud je přímo na indexu snapshot, vrátíme jeho paměť
            if (targetRecord.getType() == MemoryRecord.SNAPSHOT) {
                return Arrays.copyOf(targetRecord.getMemory(), previousMemory.length);
            }

            // Jinak musíme najít nejbližší předchozí snapshot a dopočítat změny
            ListIterator<MemoryRecord> iterator = timeline.listIterator(index);

            // Hledání nejbližšího předchozího snapshotu
            while (iterator.hasPrevious()) {
                MemoryRecord record = iterator.previous();
                if (record.getType() == MemoryRecord.SNAPSHOT) {
                    byte[] memory = Arrays.copyOf(record.getMemory(), previousMemory.length);

                    // Aplikace změn až k požadovanému indexu
                    while (iterator.hasNext()) {
                        record = iterator.next();
                        if (iterator.nextIndex() > index) {
                            break;
                        }

                        if (record.getType() == MemoryRecord.STEP) {
                            for (ChangeLog change : record.getChanges()) {
                                System.arraycopy(change.newValues, 0, memory, change.address, change.newValues.length);
                            }
                        }
                    }

                    return memory;
                }
            }

            // Pokud žádný snapshot nenalezneme, je to chyba
            throw new IllegalStateException("No snapshot found in the timeline for index=" + index);
        }
    }

    public void truncateTimeline(int index) {
        synchronized (lock) {
            while (timeline.size() > index + 1) {
                MemoryRecord record = timeline.removeLast();
                record.clear(); // uvolnění paměti
            }

            currentIndex = Math.min(currentIndex, index);
        }
    }

    public void clearTimeline() {
        synchronized (lock) {
            // Iterace přes seznam a uvolnění paměti záznamů
            for (MemoryRecord record : timeline) {
                record.clearAll(); // Uvolnění jednotlivých záznamů
            }

            // Vyprázdnění seznamu a reset stavu
            timeline.clear();
            currentIndex = -1;
            stepCount = 0;
            isRemovingOldestBlock = false;

            // Resetování paměti a CRC
            Arrays.fill(previousMemory, (byte) 0);
            Arrays.fill(previousCRCs, 0L);
            Arrays.fill(validBlocks, true);
        }
    }

    public int getTimelineSize() {
        synchronized (lock) { // Synchronizace přístupu k timeline
            return timeline.size();
        }
    }
}

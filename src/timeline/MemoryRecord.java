package timeline;

import java.util.List;


class MemoryRecord {

    public static final int SNAPSHOT = 1;
    public static final int STEP = 2;

    private final int type; // Typ záznamu
    private byte[] memory; // Pro snapshot
    private List<ChangeLog> changes; // Pro krok
    public static final int nMemBaseAddress=42;
    // Konstruktor pro snapshot
    public MemoryRecord(byte[] memory) {
        this.type = SNAPSHOT;
        this.memory = memory;
        this.changes = null;
    }

    // Konstruktor pro krok
    public MemoryRecord(List<ChangeLog> changes) {
        this.type = STEP;
        this.memory = null;
        this.changes = changes;
    }

    public int getType() {
        return type;
    }

    public byte[] getMemory() {
        return memory;
    }

    public List<ChangeLog> getChanges() {
        return changes;
    }

    // Metoda pro uvolnění paměti
    public void clear() {
        memory = null; // Uvolnění reference na pole memory

        if (changes != null) {
            // Uvolnění jednotlivých položek v seznamu
            for (int i = 0; i < changes.size(); i++) {
                changes.set(i, null);
            }
            changes.clear(); // Vymazání samotného seznamu

        }
    }

    // Metoda pro uvolnění paměti
    public void clearAll() {
        memory = null; // Uvolnění reference na pole memory

        if (changes != null) {
            // Uvolnění jednotlivých položek v seznamu
            for (int i = 0; i < changes.size(); i++) {
                ChangeLog changeLog = changes.get(i);
                if (changeLog != null) {
                    changeLog.clear(); // Uvolnění ChangeLog objektu
                }
                changes.set(i, null);
            }
            changes.clear(); // Vymazání samotného seznamu
            changes = null;
        }
    }

    public void dumpInfo(int index) {
        String header = String.format("MemoryRecord Dump (Index: %d)", index);
        int padding = (41 - header.length()) / 2; // Dynamické odsazení
        String leftPad = String.format("%" + padding + "s", "");
        String rightPad = String.format("%" + (41 - header.length() - padding) + "s", "");

        System.out.println("+=========================================+");
        System.out.printf("|%s%s%s|\n", leftPad, header, rightPad);
        System.out.println("+-----------------------------------------+");
        System.out.printf("| Type: %-33s |\n", (type == SNAPSHOT) ? "SNAPSHOT" : "STEP");

        if (type == SNAPSHOT) {
            System.out.printf("| Memory Content: %-23s |\n",
                    (memory != null ? byteArrayToString(memory) : "null"));
        } else if (type == STEP) {
            if (changes != null && !changes.isEmpty()) {
                System.out.println("| Changes:                                |");
                int i = 1;
                int memoryBaseAddress = nMemBaseAddress;
                for (ChangeLog change : changes) {
                    System.out.println("+-----------------------------------------+");
                    String detail = getChangeDetail(change, memoryBaseAddress);
                    String changeHeader = String.format("Change %d (%s)", i++, detail);
                    int changePadding = 41 - changeHeader.length() - 5;
                    System.out.printf("| *** %s%" + changePadding + "s|\n", changeHeader, ""); // Zarovnané
                    System.out.println("+-----------------------------------------+");
                    change.dumpInfo(); // Výpis změny z třídy ChangeLog
                    System.out.println("+-----------------------------------------+");
                }
            } else {
                System.out.println("| No changes recorded.                   |");
            }
        }

        System.out.println("+=========================================+\n");
    }

    private String getChangeDetail(ChangeLog change, int memoryBaseAddress) {
        int address = change.address;

        // Adresy registrů a stavů podle `saveSnapshotToArray()`
        if (address == 0) {
            return "Register I";
        } else if (address >= 1 && address <= 14) {
            // 1–14: Jednotlivé registry (Lx, Hx, Ex, Dx, Cx, Bx, Fx, Ax, L, H, E, D, C, B)
            String[] regNames = {"Lx", "Hx", "Ex", "Dx", "Cx", "Bx", "Fx", "Ax", "L", "H", "E", "D", "C", "B"};
            return "Register " + regNames[address - 1];
        } else if (address == 15) {
            return "Register IY (Low)";
        } else if (address == 16) {
            return "Register IY (High)";
        } else if (address == 17) {
            return "Register IX (Low)";
        } else if (address == 18) {
            return "Register IX (High)";
        } else if (address == 19) {
            return "IFF,NMI,INT,Halt";
        } else if (address == 20) {
            return "Register R";
        } else if (address == 21) {
            return "Register F";
        } else if (address == 22) {
            return "Register A";
        } else if (address == 23) {
            return "Register SP (Low)";
        } else if (address == 24) {
            return "Register SP (High)";
        } else if (address == 25) {
            return "Register PC (Low)";
        } else if (address == 26) {
            return "Register PC (High)";
        } else if (address == 27) {
            return "Interrupt Mode (IM)";
        } else if (address == 28) {
            return "Register MemPtr (Low)";
        } else if (address == 29) {
            return "Register MemPtr (High)";
        } else if (address >= 30 && address <= 32) {
            // Porty A0, A1, A3
            return "Port A" + (address - 30);
        } else if (address == 33) {
            return "Screen Resolution";
        } else if (address < memoryBaseAddress) {
            // T-states
            return "T-states";
        } else if (address >= memoryBaseAddress) {
            // Paměťový prostor
            int realAddress = address - memoryBaseAddress;
            return String.format("Memory 0x%04X", realAddress);
        }

        return "Unknown";
    }

    
    // Pomocná metoda pro převod pole byte na hexa řetězec
    private String byteArrayToString(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", array[i] & 0xFF));
        }
        return sb.toString();
    }
}

package timeline;

import java.util.Arrays;

class ChangeLog {

    public int address;    // Starting address of the change
    public byte[] oldValues; // Old values in the block
    public byte[] newValues; // New values in the block

    // Constructor for single-byte change
    public ChangeLog(int address, byte oldValue, byte newValue) {
        this.address = address;
        this.oldValues = new byte[]{oldValue};
        this.newValues = new byte[]{newValue};
    }

    // Constructor for block change
    public ChangeLog(int address, byte[] oldValues, byte[] newValues) {
        this.address = address;
        this.oldValues = oldValues;
        this.newValues = newValues;
    }

    // Clear method to release memory
    public void clear() {
        oldValues = null; // Release reference to old values
        newValues = null; // Release reference to new values
    }
    
 public void dumpInfo() {
    System.out.printf("| Address: 0x%04X                         |\n", address);
    System.out.println("+-----------------------------------------+");
    System.out.printf("| Old Values: %-27s |\n", byteArrayToString(oldValues));
    System.out.printf("| New Values: %-27s |\n", byteArrayToString(newValues));   
}


    // Helper method to convert byte array to unsigned string representation
    private String byteArrayToString(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(" "); // Add space between bytes
            }
            sb.append(String.format("%02X", array[i] & 0xFF)); // Convert to unsigned and format as hex
        }
        return sb.toString();
    }
}
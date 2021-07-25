//Author: tmilata
//original c++ code by Markus Fritze, ∑-Soft
//LICENSE: Freeware
package disassemblers;


public class Z80Dis {
    public static int[] Opcodes;


    public byte OpcodeLen(int p) {
        byte len = 1;
        switch (Opcodes[p]) {// Opcode
            case 0x06:          // LD B,n
            case 0x0E:          // LD C,n
            case 0x10:          // DJNZ e
            case 0x16:          // LD D,n
            case 0x18:          // JR e
            case 0x1E:          // LD E,n
            case 0x20:          // JR NZ,e
            case 0x26:          // LD H,n
            case 0x28:          // JR Z,e
            case 0x2E:          // LD L,n
            case 0x30:          // JR NC,e
            case 0x36:          // LD (HL),n
            case 0x38:          // JR C,e
            case 0x3E:          // LD A,n
            case 0xC6:          // ADD A,n
            case 0xCE:          // ADC A,n
            case 0xD3:          // OUT (n),A
            case 0xD6:          // SUB n
            case 0xDB:          // IN A,(n)
            case 0xDE:          // SBC A,n
            case 0xE6:          // AND n
            case 0xEE:          // XOR n
            case 0xF6:          // OR n
            case 0xFE:          // CP n

            case 0xCB:          // Shift-,Rotate-,Bit-Befehle
                len = 2;
                break;
            case 0x01:          // LD BC,nn'
            case 0x11:          // LD DE,nn'
            case 0x21:          // LD HL,nn'
            case 0x22:          // LD (nn'),HL
            case 0x2A:          // LD HL,(nn')
            case 0x31:          // LD SP,(nn')
            case 0x32:          // LD (nn'),A
            case 0x3A:          // LD A,(nn')
            case 0xC2:          // JP NZ,nn'
            case 0xC3:          // JP nn'
            case 0xC4:          // CALL NZ,nn'
            case 0xCA:          // JP Z,nn'
            case 0xCC:          // CALL Z,nn'
            case 0xCD:          // CALL nn'
            case 0xD2:          // JP NC,nn'
            case 0xD4:          // CALL NC,nn'
            case 0xDA:          // JP C,nn'
            case 0xDC:          // CALL C,nn'
            case 0xE2:          // JP PO,nn'
            case 0xE4:          // CALL PO,nn'
            case 0xEA:          // JP PE,nn'
            case 0xEC:          // CALL PE,nn'
            case 0xF2:          // JP P,nn'
            case 0xF4:          // CALL P,nn'
            case 0xFA:          // JP M,nn'
            case 0xFC:          // CALL M,nn'
                len = 3;
                break;
            case 0xDD:
                len = 2;
                switch (Opcodes[p + 1]) {// 2.Teil des Opcodes
                    case 0x34:          // INC (IX+d)
                    case 0x35:          // DEC (IX+d)
                    case 0x46:          // LD B,(IX+d)
                    case 0x4E:          // LD C,(IX+d)
                    case 0x56:          // LD D,(IX+d)
                    case 0x5E:          // LD E,(IX+d)
                    case 0x66:          // LD H,(IX+d)
                    case 0x6E:          // LD L,(IX+d)
                    case 0x70:          // LD (IX+d),B
                    case 0x71:          // LD (IX+d),C
                    case 0x72:          // LD (IX+d),D
                    case 0x73:          // LD (IX+d),E
                    case 0x74:          // LD (IX+d),H
                    case 0x75:          // LD (IX+d),L
                    case 0x77:          // LD (IX+d),A
                    case 0x7E:          // LD A,(IX+d)
                    case 0x86:          // ADD A,(IX+d)
                    case 0x8E:          // ADC A,(IX+d)
                    case 0x96:          // SUB A,(IX+d)
                    case 0x9E:          // SBC A,(IX+d)
                    case 0xA6:          // AND (IX+d)
                    case 0xAE:          // XOR (IX+d)
                    case 0xB6:          // OR (IX+d)
                    case 0xBE:          // CP (IX+d)
                        len = 3;
                        break;
                    case 0x21:          // LD IX,nn'
                    case 0x22:          // LD (nn'),IX
                    case 0x2A:          // LD IX,(nn')
                    case 0x36:          // LD (IX+d),n
                    case 0xCB:          // Rotation (IX+d)
                        len = 4;
                        break;
                }
                break;
            case 0xED:
                len = 2;
                switch (Opcodes[p + 1]) {// 2.Teil des Opcodes
                    case 0x43:          // LD (nn'),BC
                    case 0x4B:          // LD BC,(nn')
                    case 0x53:          // LD (nn'),DE
                    case 0x5B:          // LD DE,(nn')
                    case 0x73:          // LD (nn'),SP
                    case 0x7B:          // LD SP,(nn')
                        len = 4;
                        break;
                }
                break;
            case 0xFD:
                len = 2;
                switch (Opcodes[p + 1]) {// 2.Teil des Opcodes
                    case 0x34:          // INC (IY+d)
                    case 0x35:          // DEC (IY+d)
                    case 0x46:          // LD B,(IY+d)
                    case 0x4E:          // LD C,(IY+d)
                    case 0x56:          // LD D,(IY+d)
                    case 0x5E:          // LD E,(IY+d)
                    case 0x66:          // LD H,(IY+d)
                    case 0x6E:          // LD L,(IY+d)
                    case 0x70:          // LD (IY+d),B
                    case 0x71:          // LD (IY+d),C
                    case 0x72:          // LD (IY+d),D
                    case 0x73:          // LD (IY+d),E
                    case 0x74:          // LD (IY+d),H
                    case 0x75:          // LD (IY+d),L
                    case 0x77:          // LD (IY+d),A
                    case 0x7E:          // LD A,(IY+d)
                    case 0x86:          // ADD A,(IY+d)
                    case 0x8E:          // ADC A,(IY+d)
                    case 0x96:          // SUB A,(IY+d)
                    case 0x9E:          // SBC A,(IY+d)
                    case 0xA6:          // AND (IY+d)
                    case 0xAE:          // XOR (IY+d)
                    case 0xB6:          // OR (IY+d)
                    case 0xBE:          // CP (IY+d)
                        len = 3;
                        break;
                    case 0x21:          // LD IY,nn'
                    case 0x22:          // LD (nn'),IY
                    case 0x2A:          // LD IY,(nn')
                    case 0x36:          // LD (IY+d),n
                    case 0xCB:          // Rotation,Bitop (IY+d)
                        len = 4;
                        break;
                }
                break;
        }
        return (len);
    }
    
   
    public String Disassemble(int adr) {
        String s="";
        int a = Opcodes[adr];
        int d = (a >> 3) & 7;
        int e = a & 7;
        String[] reg = {"B", "C", "D", "E", "H", "L", "(HL)", "A"};
        String[] dreg = {"BC", "DE", "HL", "SP"};
        String[] cond = {"NZ", "Z", "NC", "C", "PO", "PE", "P", "M"};
        String[] arith = {"ADD A,", "ADC A,", "SUB ", "SBC A,", "AND ", "XOR ", "OR ", "CP "};
        String stemp = "";      // temp.String fÃ¼r sprintf()
        String ireg = "";        // temp.Indexregister

        switch (a & 0xC0) {
            case 0x00:
                switch (e) {
                    case 0x00:
                        switch (d) {
                            case 0x00:
                                s= "NOP";
                                break;
                            case 0x01:
                                s= "EX AF,AF'";
                                break;
                            case 0x02:
                                s= "DJNZ ";
                                stemp=String.format( "#%04X", adr + 2 + (byte) Opcodes[adr + 1]);
                                s=s+ stemp;
                                break;
                            case 0x03:
                                s= "JR ";
                                stemp=String.format( "#%04X", adr + 2 + (byte) Opcodes[adr + 1]);
                                s=s+ stemp;
                                break;
                            default:
                                s= "JR ";
                                s=s+ cond[d & 3];
                                s=s+ ",";
                                stemp=String.format( "#%04X", adr + 2 + (byte) Opcodes[adr + 1]);
                                s=s+ stemp;
                                break;
                        }
                        break;
                    case 0x01:
                        if ((a & 8) != 0) {
                            s= "ADD HL,";
                            s=s+ dreg[d >> 1];
                        } else {
                            s= "LD ";
                            s=s+ dreg[d >> 1];
                            s=s+ ",";
                            stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                            s=s+ stemp;
                        }
                        break;
                    case 0x02:
                        switch (d) {
                            case 0x00:
                                s= "LD (BC),A";
                                break;
                            case 0x01:
                                s= "LD A,(BC)";
                                break;
                            case 0x02:
                                s= "LD (DE),A";
                                break;
                            case 0x03:
                                s= "LD A,(DE)";
                                break;
                            case 0x04:
                                s= "LD (";
                                stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                s=s+ stemp;
                                s=s+ "),HL";
                                break;
                            case 0x05:
                                s= "LD HL,(";
                                stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                s=s+ stemp;
                                s=s+ ")";
                                break;
                            case 0x06:
                                s= "LD (";
                                stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                s=s+ stemp;
                                s=s+ "),A";
                                break;
                            case 0x07:
                                s= "LD A,(";
                                stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                s=s+ stemp;
                                s=s+ ")";
                                break;
                        }
                        break;
                    case 0x03:
                        if ((a & 8) != 0) {
                            s= "DEC ";
                        } else {
                            s= "INC ";
                        }
                        s=s+ dreg[d >> 1];
                        break;
                    case 0x04:
                        s= "INC ";
                        s=s+ reg[d];
                        break;
                    case 0x05:
                        s= "DEC ";
                        s=s+ reg[d];
                        break;
                    case 0x06:              // LD   d,n
                        s= "LD ";
                        s=s+ reg[d];
                        s=s+ ",";
                        stemp=String.format("#%02X", Opcodes[adr + 1]);
                        s=s+ stemp;
                        break;
                    case 0x07: {
                        String[] str = {"RLCA", "RRCA", "RLA", "RRA", "DAA", "CPL", "SCF", "CCF"};
                        s= str[d];
                    }
                    break;
                }
                break;
            case 0x40:                          // LD   d,s
                if ((d == e) && (d == 6)) {
                    s= "HALT";
                } else {
                    s= "LD ";
                    s=s+ reg[d];
                    s=s+ ",";
                    s=s+ reg[e];
                }
                break;
            case 0x80:
                s= arith[d];
                s=s+ reg[e];
                break;
            case 0xC0:
                switch (e) {
                    case 0x00:
                        s= "RET ";
                        s=s+ cond[d];
                        break;
                    case 0x01:
                        if ((d & 1) != 0) {
                            switch (d >> 1) {
                                case 0x00:
                                    s= "RET";
                                    break;
                                case 0x01:
                                    s= "EXX";
                                    break;
                                case 0x02:
                                    s= "JP (HL)";
                                    break;
                                case 0x03:
                                    s= "LD SP,HL";
                                    break;
                            }
                        } else {
                            s= "POP ";
                            if ((d >> 1) == 3) {
                                s=s+ "AF";
                            } else {
                                s=s+ dreg[d >> 1];
                            }
                        }
                        break;
                    case 0x02:
                        s= "JP ";
                        s=s+ cond[d];
                        s=s+ ",";
                        stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                        s=s+ stemp;
                        break;
                    case 0x03:
                        switch (d) {
                            case 0x00:
                                s= "JP ";
                                stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                s=s+ stemp;
                                break;
                            case 0x01:                  // 0xCB
                                a = Opcodes[++adr];     // Erweiterungsopcode holen
                                d = (a >> 3) & 7;
                                e = a & 7;
                                stemp = "";           // temp.String = 1 Zeichen
                                switch (a & 0xC0) {
                                    case 0x00: {
                                        String[] str = {"RLC", "RRC", "RL", "RR", "SLA", "SRA", "???", "SRL"};
                                        s= str[d];
                                    }
                                    s=s+ " ";
                                    s=s+ reg[e];
                                    break;
                                    case 0x40:
                                        s= "BIT ";
                                        stemp = String.format("%d", d);
                                        s=s+ stemp;
                                        s=s+ ",";
                                        s=s+ reg[e];
                                        break;
                                    case 0x80:
                                        s= "RES ";
                                        stemp = String.format("%d", d);
                                        s=s+ stemp;
                                        s=s+ ",";
                                        s=s+ reg[e];
                                        break;
                                    case 0xC0:
                                        s= "SET ";
                                        stemp = String.format("%d", d);
                                        s=s+ stemp;
                                        s=s+ ",";
                                        s=s+ reg[e];
                                        break;
                                }
                                break;
                            case 0x02:
                                s= "OUT (";
                                stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                s=s+ stemp;
                                s=s+ "),A";
                                break;
                            case 0x03:
                                s= "IN A,(";
                                stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                s=s+ stemp;
                                s=s+ ")";
                                break;
                            case 0x04:
                                s= "EX (SP),HL";
                                break;
                            case 0x05:
                                s= "EX DE,HL";
                                break;
                            case 0x06:
                                s= "DI";
                                break;
                            case 0x07:
                                s= "EI";
                                break;
                        }
                        break;
                    case 0x04:
                        s= "CALL ";
                        s=s+ cond[d];
                        s=s+ ",";
                        stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                        s=s+ stemp;
                        break;
                    case 0x05:
                        if ((d & 1) != 0) {
                            switch (d >> 1) {
                                case 0x00:
                                    s= "CALL ";
                                    stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                    s=s+ stemp;
                                    break;
                                case 0x02:              // 0xED
                                    a = Opcodes[++adr]; // Erweiterungsopcode holen
                                    d = (a >> 3) & 7;
                                    e = a & 7;
                                    switch (a & 0xC0) {
                                        case 0x40:
                                            switch (e) {
                                                case 0x00:
                                                    s= "IN ";
                                                    s=s+ reg[d];
                                                    s=s+ ",(C)";
                                                    break;
                                                case 0x01:
                                                    s= "OUT (C),";
                                                    s=s+ reg[d];
                                                    break;
                                                case 0x02:
                                                    if ((d & 1) != 0) {
                                                        s= "ADC";
                                                    } else {
                                                        s= "SBC";
                                                    }
                                                    s=s+ " HL,";
                                                    s=s+ dreg[d >> 1];
                                                    break;
                                                case 0x03:
                                                    if ((d & 1) != 0) {
                                                        s= "LD ";
                                                        s=s+ dreg[d >> 1];
                                                        s=s+ ",(";
                                                        stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                                        s=s+ stemp;
                                                        s=s+ ")";
                                                    } else {
                                                        s= "LD (";
                                                        stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                                        s=s+ stemp;
                                                        s=s+ "),";
                                                        s=s+ dreg[d >> 1];
                                                    }
                                                    break;
                                                case 0x04: {
                                                    String[] str = {"NEG", "???", "???", "???", "???", "???", "???", "???"};
                                                    s= str[d];
                                                }
                                                break;
                                                case 0x05: {
                                                    String[] str = {"RETN", "RETI", "???", "???", "???", "???", "???", "???"};
                                                    s= str[d];
                                                }
                                                break;
                                                case 0x06:
                                                    s= "IM ";
                                                    stemp = String.format("%d", d);
                                                    s=s+ stemp;
                                                    break;
                                                case 0x07: {
                                                    String[] str = {"LD I,A", "???", "LD A,I", "???", "RRD", "RLD", "???", "???"};
                                                    s= str[d];
                                                }
                                                break;
                                            }
                                            break;
                                        case 0x80: {
                                            String[] str = {"LDI", "CPI", "INI", "OUTI", "???", "???", "???", "???",
                                                "LDD", "CPD", "IND", "OUTD", "???", "???", "???", "???",
                                                "LDIR", "CPIR", "INIR", "OTIR", "???", "???", "???", "???",
                                                "LDDR", "CPDR", "INDR", "OTDR", "???", "???", "???", "???"};
                                            s= str[a & 0x1F];
                                        }
                                        break;
                                    }
                                    break;
                                default:                // 0x01 (0xDD) = IX, 0x03 (0xFD) = IY
                                    if ((a & 0x20) != 0) {
                                        ireg= "IY";
                                    } else {
                                        ireg= "IX";
                                    }                                    
                                    a = Opcodes[++adr]; // Erweiterungsopcode holen
                                    switch (a) {
                                        case 0x09:
                                            s= "ADD ";
                                            s=s+ ireg;
                                            s=s+ ",BC";
                                            break;
                                        case 0x19:
                                            s= "ADD ";
                                            s=s+ ireg;
                                            s=s+ ",DE";
                                            break;
                                        case 0x21:
                                            s= "LD ";
                                            s=s+ ireg;
                                            s=s+ ",";
                                            stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                            s=s+ stemp;
                                            break;
                                        case 0x22:
                                            s= "LD (";
                                            stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                            s=s+ stemp;
                                            s=s+ "),";
                                            s=s+ ireg;
                                            break;
                                        case 0x23:
                                            s= "INC ";
                                            s=s+ ireg;
                                            break;
                                        case 0x29:
                                            s= "ADD ";
                                            s=s+ ireg;
                                            s=s+ ",";
                                            s=s+ ireg;
                                            break;
                                        case 0x2A:
                                            s= "LD ";
                                            s=s+ ireg;
                                            s=s+ ",(";
                                            stemp=String.format( "#%04X", Opcodes[adr + 1] + (Opcodes[adr + 2] << 8));
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x2B:
                                            s= "DEC ";
                                            s=s+ ireg;
                                            break;
                                        case 0x34:
                                            s= "INC (";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x35:
                                            s= "DEC (";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x36:
                                            s= "LD (";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ "),";
                                            stemp=String.format( "#%02X", Opcodes[adr + 2]);
                                            s=s+ stemp;
                                            break;
                                        case 0x39:
                                            s= "ADD ";
                                            s=s+ ireg;
                                            s=s+ ",SP";
                                            break;
                                        case 0x46:
                                        case 0x4E:
                                        case 0x56:
                                        case 0x5E:
                                        case 0x66:
                                        case 0x6E:
                                            s= "LD ";
                                            s=s+ reg[(a >> 3) & 7];
                                            s=s+ ",(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x70:
                                        case 0x71:
                                        case 0x72:
                                        case 0x73:
                                        case 0x74:
                                        case 0x75:
                                        case 0x77:
                                            s= "LD (";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ "),";
                                            s=s+ reg[a & 7];
                                            break;
                                        case 0x7E:
                                            s= "LD A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x86:
                                            s= "ADD A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x8E:
                                            s= "ADC A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x96:
                                            s= "SUB (";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0x9E:
                                            s= "SBC A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0xA6:
                                            s= "AND A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0xAE:
                                            s= "XOR A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0xB6:
                                            s= "OR A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0xBE:
                                            s= "CP A,(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                        case 0xE1:
                                            s= "POP ";
                                            s=s+ ireg;
                                            break;
                                        case 0xE3:
                                            s= "EX (SP),";
                                            s=s+ ireg;
                                            break;
                                        case 0xE5:
                                            s= "PUSH ";
                                            s=s+ ireg;
                                            break;
                                        case 0xE9:
                                            s= "JP (";
                                            s=s+ ireg;
                                            s=s+ ")";
                                            break;
                                        case 0xF9:
                                            s= "LD SP,";
                                            s=s+ ireg;
                                            break;
                                        case 0xCB:
                                            a = Opcodes[adr + 2]; // weiteren Unteropcode
                                            d = (a >> 3) & 7;
                                            stemp = "";
                                            switch (a & 0xC0) {
                                                case 0x00: {
                                                    String[] str = {"RLC", "RRC", "RL", "RR", "SLA", "SRA", "???", "SRL"};
                                                    s= str[d];
                                                }
                                                s=s+ " ";
                                                break;
                                                case 0x40:
                                                    s= "BIT ";
                                                    stemp = String.format("%d", d);
                                                    s=s+ stemp;
                                                    s=s+ ",";
                                                    break;
                                                case 0x80:
                                                    s= "RES ";
                                                    stemp = String.format("%d", d);
                                                    s=s+ stemp;
                                                    s=s+ ",";
                                                    break;
                                                case 0xC0:
                                                    s= "SET ";
                                                    stemp = String.format("%d", d);
                                                    s=s+ stemp;
                                                    s=s+ ",";
                                                    break;
                                            }
                                            s=s+ "(";
                                            s=s+ ireg;
                                            s=s+ "+";
                                            stemp=String.format( "#%02X", Opcodes[adr + 1]);
                                            s=s+ stemp;
                                            s=s+ ")";
                                            break;
                                    }
                                    break;
                            }
                        } else {
                            s= "PUSH ";
                            if ((d >> 1) == 3) {
                                s=s+ "AF";
                            } else {
                                s=s+ dreg[d >> 1];
                            }
                        }
                        break;
                    case 0x06:
                        s= arith[d];
                        stemp=String.format( "#%02X", Opcodes[adr + 1]);
                        s=s+ stemp;
                        break;
                    case 0x07:
                        s= "RST ";
                        stemp=String.format( "#%02X", a & 0x38);
                        s=s+ stemp;
                        break;
                }
                break;
        }
        return s;
    }
  /*  
    public static void main(String[] args) {
        Z80Dis z80d=new Z80Dis();
        Opcodes=new int[5];
        String retval="";
        Opcodes[0]=0xFD;
        Opcodes[1]=0x71;
        Opcodes[2]=0x05;
        retval=z80d.Disassemble(0);
        System.out.println(retval);
    }
     */
     
}

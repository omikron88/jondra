//Author: tmilata
//original c++ code by Markus Fritze, ∑-Soft
//and Robson Couto 2017
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


 public String Disassemble(int pc) {
  String s="";
  int instruction=Opcodes[pc];
  int inst_bytes=1;
  int bit_flag=0,ix_flag=0,ix_bit_flag=0,extd_flag=0,iy_flag=0,iy_bit_flag=0;
  switch (instruction) {
    case 0x00: s=s+String.format("NOP"); break;
    case 0x01: s=s+String.format("LD BC,#%02X%02X ",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x02: s=s+String.format("LD (BC),A"); break;
    case 0x03: s=s+String.format("INC BC"); break;
    case 0x04: s=s+String.format("INC B"); break;
    case 0x05: s=s+String.format("DEC B"); break;
    case 0x06: s=s+String.format("LD B,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x07: s=s+String.format("RLCA"); break;
    case 0x08: s=s+String.format("EX AF,AF'"); break;
    case 0x09: s=s+String.format("ADD HL,BC"); break;
    case 0x0A: s=s+String.format("LD A,(BC)"); break;
    case 0x0B: s=s+String.format("DEC BC"); break;
    case 0x0C: s=s+String.format("INC C"); break;
    case 0x0D: s=s+String.format("DEC C"); break;
    case 0x0E: s=s+String.format("LD C,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x0F: s=s+String.format("RRCA"); break;

    case 0x10: s=s+String.format("DJNZ,#%04X",pc + 2 + (byte) Opcodes[pc + 1]); inst_bytes=2; break;
    case 0x11: s=s+String.format("LD DE,#%02X%02X ",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x12: s=s+String.format("LD (DE),A"); break;
    case 0x13: s=s+String.format("INC DE"); break;
    case 0x14: s=s+String.format("INC D"); break;
    case 0x15: s=s+String.format("DEC D"); break;
    case 0x16: s=s+String.format("LD D,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x17: s=s+String.format("RLA"); break;
    case 0x18: s=s+String.format("JR #%04X", pc + 2 + (byte) Opcodes[pc + 1] ); inst_bytes=2; break;
    case 0x19: s=s+String.format("ADD HL,DE"); break;
    case 0x1A: s=s+String.format("LD A,(DE)"); break;
    case 0x1B: s=s+String.format("DEC DE"); break;
    case 0x1C: s=s+String.format("INC E"); break;
    case 0x1D: s=s+String.format("DEC E"); break;
    case 0x1E: s=s+String.format("LD E,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x1F: s=s+String.format("RRA"); break;

    case 0x20: s=s+String.format("JR NZ,#%04X",pc + 2 + (byte) Opcodes[pc + 1]); inst_bytes=2; break;
    case 0x21: s=s+String.format("LD HL,#%02X%02X ",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x22: s=s+String.format("LD (#%02X%02X),HL",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x23: s=s+String.format("INC HL"); break;
    case 0x24: s=s+String.format("INC H"); break;
    case 0x25: s=s+String.format("DEC H"); break;
    case 0x26: s=s+String.format("LD H,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x27: s=s+String.format("DAA"); break;
    case 0x28: s=s+String.format("JR Z,#%04X",pc + 2 + (byte) Opcodes[pc + 1]); inst_bytes=2; break;
    case 0x29: s=s+String.format("ADD HL,HL"); break;
    case 0x2A: s=s+String.format("LD HL,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x2B: s=s+String.format("DEC HL"); break;
    case 0x2C: s=s+String.format("INC L"); break;
    case 0x2D: s=s+String.format("DEC L"); break;
    case 0x2E: s=s+String.format("LD L,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x2F: s=s+String.format("CPL"); break;

    case 0x30: s=s+String.format("JR NC,#%04X",pc + 2 + (byte) Opcodes[pc + 1]); inst_bytes=2; break;
    case 0x31: s=s+String.format("LD SP,#%02X%02X ",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x32: s=s+String.format("LD (#%02X%02X),A",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x33: s=s+String.format("INC SP"); break;
    case 0x34: s=s+String.format("INC (HL)"); break;
    case 0x35: s=s+String.format("DEC (HL)"); break;
    case 0x36: s=s+String.format("LD (HL),#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x37: s=s+String.format("SCF"); break;
    case 0x38: s=s+String.format("JR C,#%04X",pc + 2 + (byte) Opcodes[pc + 1]); inst_bytes=2; break;
    case 0x39: s=s+String.format("ADD HL,SP"); break;
    case 0x3A: s=s+String.format("LD A,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0x3B: s=s+String.format("DEC SP"); break;
    case 0x3C: s=s+String.format("INC A"); break;
    case 0x3D: s=s+String.format("DEC A"); break;
    case 0x3E: s=s+String.format("LD A,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0x3F: s=s+String.format("CPF"); break;

    case 0x40: s=s+String.format("LD B,B"); break;
    case 0x41: s=s+String.format("LD B,C"); break;
    case 0x42: s=s+String.format("LD B,D"); break;
    case 0x43: s=s+String.format("LD B,E"); break;
    case 0x44: s=s+String.format("LD B,H"); break;
    case 0x45: s=s+String.format("LD B,L"); break;
    case 0x46: s=s+String.format("LD B,(HL)"); break;
    case 0x47: s=s+String.format("LD B,A"); break;
    case 0x48: s=s+String.format("LD C,B"); break;
    case 0x49: s=s+String.format("LD C,C"); break;
    case 0x4A: s=s+String.format("LD C,D"); break;
    case 0x4B: s=s+String.format("LD C,E"); break;
    case 0x4C: s=s+String.format("LD C,H"); break;
    case 0x4D: s=s+String.format("LD C,L"); break;
    case 0x4E: s=s+String.format("LD C,(HL)"); break;
    case 0x4F: s=s+String.format("LD C,A"); break;

    case 0x50: s=s+String.format("LD D,B"); break;
    case 0x51: s=s+String.format("LD D,C"); break;
    case 0x52: s=s+String.format("LD D,D"); break;
    case 0x53: s=s+String.format("LD D,E"); break;
    case 0x54: s=s+String.format("LD D,H"); break;
    case 0x55: s=s+String.format("LD D,L"); break;
    case 0x56: s=s+String.format("LD D,(HL)"); break;
    case 0x57: s=s+String.format("LD D,A"); break;
    case 0x58: s=s+String.format("LD E,B"); break;
    case 0x59: s=s+String.format("LD E,C"); break;
    case 0x5A: s=s+String.format("LD E,D"); break;
    case 0x5B: s=s+String.format("LD E,E"); break;
    case 0x5C: s=s+String.format("LD E,H"); break;
    case 0x5D: s=s+String.format("LD E,L"); break;
    case 0x5E: s=s+String.format("LD E,(HL)"); break;
    case 0x5F: s=s+String.format("LD E,A"); break;

    case 0x60: s=s+String.format("LD H,B"); break;
    case 0x61: s=s+String.format("LD H,C"); break;
    case 0x62: s=s+String.format("LD H,D"); break;
    case 0x63: s=s+String.format("LD H,E"); break;
    case 0x64: s=s+String.format("LD H,H"); break;
    case 0x65: s=s+String.format("LD H,L"); break;
    case 0x66: s=s+String.format("LD H,(HL)"); break;
    case 0x67: s=s+String.format("LD H,A"); break;
    case 0x68: s=s+String.format("LD L,B"); break;
    case 0x69: s=s+String.format("LD L,C"); break;
    case 0x6A: s=s+String.format("LD L,D"); break;
    case 0x6B: s=s+String.format("LD L,E"); break;
    case 0x6C: s=s+String.format("LD L,H"); break;
    case 0x6D: s=s+String.format("LD L,L"); break;
    case 0x6E: s=s+String.format("LD L,(HL)"); break;
    case 0x6F: s=s+String.format("LD L,A"); break;

    case 0x70: s=s+String.format("LD (HL),B"); break;
    case 0x71: s=s+String.format("LD (HL),C"); break;
    case 0x72: s=s+String.format("LD (HL),D"); break;
    case 0x73: s=s+String.format("LD (HL),E"); break;
    case 0x74: s=s+String.format("LD (HL),H"); break;
    case 0x75: s=s+String.format("LD (HL),L"); break;
    case 0x76: s=s+String.format("HALT"); break;
    case 0x77: s=s+String.format("LD (HL),A"); break;
    case 0x78: s=s+String.format("LD A,B"); break;
    case 0x79: s=s+String.format("LD A,C"); break;
    case 0x7A: s=s+String.format("LD A,D"); break;
    case 0x7B: s=s+String.format("LD A,E"); break;
    case 0x7C: s=s+String.format("LD A,H"); break;
    case 0x7D: s=s+String.format("LD A,L"); break;
    case 0x7E: s=s+String.format("LD A,(HL)"); break;
    case 0x7F: s=s+String.format("LD A,A"); break;

    case 0x80: s=s+String.format("ADD A,B"); break;
    case 0x81: s=s+String.format("ADD A,C"); break;
    case 0x82: s=s+String.format("ADD A,D"); break;
    case 0x83: s=s+String.format("ADD A,E"); break;
    case 0x84: s=s+String.format("ADD A,H"); break;
    case 0x85: s=s+String.format("ADD A,L"); break;
    case 0x86: s=s+String.format("ADD A,(HL)"); break;
    case 0x87: s=s+String.format("ADD A,A"); break;
    case 0x88: s=s+String.format("ADC A,B"); break;
    case 0x89: s=s+String.format("ADC A,C"); break;
    case 0x8A: s=s+String.format("ADC A,D"); break;
    case 0x8B: s=s+String.format("ADC A,E"); break;
    case 0x8C: s=s+String.format("ADC A,H"); break;
    case 0x8D: s=s+String.format("ADC A,L"); break;
    case 0x8E: s=s+String.format("ADC A,(HL)"); break;
    case 0x8F: s=s+String.format("ADC A,A"); break;

    case 0x90: s=s+String.format("SUB B"); break;
    case 0x91: s=s+String.format("SUB C"); break;
    case 0x92: s=s+String.format("SUB D"); break;
    case 0x93: s=s+String.format("SUB E"); break;
    case 0x94: s=s+String.format("SUB H"); break;
    case 0x95: s=s+String.format("SUB L"); break;
    case 0x96: s=s+String.format("SUB (HL)"); break;
    case 0x97: s=s+String.format("SUB A"); break;
    case 0x98: s=s+String.format("SBC A,B"); break;
    case 0x99: s=s+String.format("SBC A,C"); break;
    case 0x9A: s=s+String.format("SBC A,D"); break;
    case 0x9B: s=s+String.format("SBC A,E"); break;
    case 0x9C: s=s+String.format("SBC A,H"); break;
    case 0x9D: s=s+String.format("SBC A,L"); break;
    case 0x9E: s=s+String.format("SBC A,(HL)"); break;
    case 0x9F: s=s+String.format("SBC A,A"); break;

    case 0xA0: s=s+String.format("AND B"); break;
    case 0xA1: s=s+String.format("AND C"); break;
    case 0xA2: s=s+String.format("AND D"); break;
    case 0xA3: s=s+String.format("AND E"); break;
    case 0xA4: s=s+String.format("AND H"); break;
    case 0xA5: s=s+String.format("AND L"); break;
    case 0xA6: s=s+String.format("AND (HL)"); break;
    case 0xA7: s=s+String.format("AND A"); break;
    case 0xA8: s=s+String.format("XOR B"); break;
    case 0xA9: s=s+String.format("XOR C"); break;
    case 0xAA: s=s+String.format("XOR D"); break;
    case 0xAB: s=s+String.format("XOR E"); break;
    case 0xAC: s=s+String.format("XOR H"); break;
    case 0xAD: s=s+String.format("XOR L"); break;
    case 0xAE: s=s+String.format("XOR (HL)"); break;
    case 0xAF: s=s+String.format("XOR A"); break;

    case 0xB0: s=s+String.format("OR B"); break;
    case 0xB1: s=s+String.format("OR C"); break;
    case 0xB2: s=s+String.format("OR D"); break;
    case 0xB3: s=s+String.format("OR E"); break;
    case 0xB4: s=s+String.format("OR H"); break;
    case 0xB5: s=s+String.format("OR L"); break;
    case 0xB6: s=s+String.format("OR (HL)"); break;
    case 0xB7: s=s+String.format("OR A"); break;
    case 0xB8: s=s+String.format("CP B"); break;
    case 0xB9: s=s+String.format("CP C"); break;
    case 0xBA: s=s+String.format("CP D"); break;
    case 0xBB: s=s+String.format("CP E"); break;
    case 0xBC: s=s+String.format("CP H"); break;
    case 0xBD: s=s+String.format("CP L"); break;
    case 0xBE: s=s+String.format("CP (HL)"); break;
    case 0xBF: s=s+String.format("CP A"); break;

    case 0xC0: s=s+String.format("RET NZ"); break;
    case 0xC1: s=s+String.format("POP BC"); break;
    case 0xC2: s=s+String.format("JP NZ,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xC3: s=s+String.format("JP #%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xC4: s=s+String.format("CALL NZ,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xC5: s=s+String.format("PUSH BC"); break;
    case 0xC6: s=s+String.format("ADD A,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xC7: s=s+String.format("RST 00h"); break;
    case 0xC8: s=s+String.format("RET Z"); break;
    case 0xC9: s=s+String.format("RET"); break;
    case 0xCA: s=s+String.format("JP Z,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xCB: bit_flag=1; break;
    case 0xCC: s=s+String.format("CALL Z,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xCD: s=s+String.format("CALL #%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xCE: s=s+String.format("ADC A,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xCF: s=s+String.format("RST 08h"); break;

    case 0xD0: s=s+String.format("RET NC"); break;
    case 0xD1: s=s+String.format("POP DE"); break;
    case 0xD2: s=s+String.format("JP NC,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xD3: s=s+String.format("OUT (#%02X),A ",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xD4: s=s+String.format("CALL NC,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xD5: s=s+String.format("PUSH DE"); break;
    case 0xD6: s=s+String.format("SUB #%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xD7: s=s+String.format("RST 10h"); break;
    case 0xD8: s=s+String.format("RET C"); break;
    case 0xD9: s=s+String.format("EXX"); break;
    case 0xDA: s=s+String.format("JP C,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xDB: s=s+String.format("IN A,(#%02X)",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xDC: s=s+String.format("CALL C,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xDD: ix_flag=1; break;
    case 0xDE: s=s+String.format("SBC A,#%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xDF: s=s+String.format("RST 18h"); break;

    case 0xE0: s=s+String.format("RET PO"); break;
    case 0xE1: s=s+String.format("POP HL"); break;
    case 0xE2: s=s+String.format("JP PO,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xE3: s=s+String.format("EX (SP),HL "); break;
    case 0xE4: s=s+String.format("CALL PO,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xE5: s=s+String.format("PUSH HL"); break;
    case 0xE6: s=s+String.format("AND #%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xE7: s=s+String.format("RST 20h"); break;
    case 0xE8: s=s+String.format("RET PE"); break;
    case 0xE9: s=s+String.format("JP (HL)"); break;
    case 0xEA: s=s+String.format("JP PE,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xEB: s=s+String.format("EX DE,HL"); break;
    case 0xEC: s=s+String.format("CALL PE,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xED: extd_flag=1; break;
    case 0xEE: s=s+String.format("XOR #%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xEF: s=s+String.format("RST 28h"); break;

    case 0xF0: s=s+String.format("RET P"); break;
    case 0xF1: s=s+String.format("POP AF"); break;
    case 0xF2: s=s+String.format("JP P,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xF3: s=s+String.format("DI"); break;
    case 0xF4: s=s+String.format("CALL P,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xF5: s=s+String.format("PUSH AF"); break;
    case 0xF6: s=s+String.format("OR #%02X",Opcodes[pc+1]); inst_bytes=2; break;
    case 0xF7: s=s+String.format("RST 30h"); break;
    case 0xF8: s=s+String.format("RET M"); break;
    case 0xF9: s=s+String.format("LD SP,HL"); break;
    case 0xFA: s=s+String.format("JP M,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xFB: s=s+String.format("EI"); break;
    case 0xFC: s=s+String.format("CALL M,#%02X%02X",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=3; break;
    case 0xFD: iy_flag=1; break;
    case 0xFE: s=s+String.format("CP #%02X",Opcodes[pc+1]);inst_bytes=2; break;
    case 0xFF: s=s+String.format("RST 38h"); break;

    default: s=s+String.format(" ILLEGAL INSTRUCTION"); break;
  }

  if(bit_flag!=0){
    inst_bytes=2;
    switch (Opcodes[pc+1]) {
      case 0x00: s=s+String.format("RLC B"); break;
      case 0x01: s=s+String.format("RLC C"); break;
      case 0x02: s=s+String.format("RLC D"); break;
      case 0x03: s=s+String.format("RLC E"); break;
      case 0x04: s=s+String.format("RLC H"); break;
      case 0x05: s=s+String.format("RLC L"); break;
      case 0x06: s=s+String.format("RLC (HL)"); break;
      case 0x07: s=s+String.format("RLC A"); break;
      case 0x08: s=s+String.format("RRC B"); break;
      case 0x09: s=s+String.format("RRC C"); break;
      case 0x0A: s=s+String.format("RRC D"); break;
      case 0x0B: s=s+String.format("RRC E"); break;
      case 0x0C: s=s+String.format("RRC H"); break;
      case 0x0D: s=s+String.format("RRC L"); break;
      case 0x0E: s=s+String.format("RRC (HL)"); break;
      case 0x0F: s=s+String.format("RRC A"); break;

      case 0x10: s=s+String.format("RL B"); break;
      case 0x11: s=s+String.format("RL C"); break;
      case 0x12: s=s+String.format("RL D"); break;
      case 0x13: s=s+String.format("RL E"); break;
      case 0x14: s=s+String.format("RL H"); break;
      case 0x15: s=s+String.format("RL L"); break;
      case 0x16: s=s+String.format("RL (HL)"); break;
      case 0x17: s=s+String.format("RL A"); break;
      case 0x18: s=s+String.format("RR B"); break;
      case 0x19: s=s+String.format("RR C"); break;
      case 0x1A: s=s+String.format("RR D"); break;
      case 0x1B: s=s+String.format("RR E"); break;
      case 0x1C: s=s+String.format("RR H"); break;
      case 0x1D: s=s+String.format("RR L"); break;
      case 0x1E: s=s+String.format("RR (HL)"); break;
      case 0x1F: s=s+String.format("RR A"); break;

      case 0x20: s=s+String.format("SLA B"); break;
      case 0x21: s=s+String.format("SLA C"); break;
      case 0x22: s=s+String.format("SLA D"); break;
      case 0x23: s=s+String.format("SLA E"); break;
      case 0x24: s=s+String.format("SLA H"); break;
      case 0x25: s=s+String.format("SLA L"); break;
      case 0x26: s=s+String.format("SLA (HL)"); break;
      case 0x27: s=s+String.format("SLA A"); break;
      case 0x28: s=s+String.format("SRA B"); break;
      case 0x29: s=s+String.format("SRA C"); break;
      case 0x2A: s=s+String.format("SRA D"); break;
      case 0x2B: s=s+String.format("SRA E"); break;
      case 0x2C: s=s+String.format("SRA H"); break;
      case 0x2D: s=s+String.format("SRA L"); break;
      case 0x2E: s=s+String.format("SRA (HL)"); break;
      case 0x2F: s=s+String.format("SRA A"); break;

      case 0x30: s=s+String.format("SLL B"); break;
      case 0x31: s=s+String.format("SLL C"); break;
      case 0x32: s=s+String.format("SLL D"); break;
      case 0x33: s=s+String.format("SLL E"); break;
      case 0x34: s=s+String.format("SLL H"); break;
      case 0x35: s=s+String.format("SLL L"); break;
      case 0x36: s=s+String.format("SLL (HL)"); break;
      case 0x37: s=s+String.format("SLA A"); break;
      case 0x38: s=s+String.format("SRL B"); break;
      case 0x39: s=s+String.format("SRL C"); break;
      case 0x3A: s=s+String.format("SRL D"); break;
      case 0x3B: s=s+String.format("SRL E"); break;
      case 0x3C: s=s+String.format("SRL H"); break;
      case 0x3D: s=s+String.format("SRL L"); break;
      case 0x3E: s=s+String.format("SRL (HL)"); break;
      case 0x3F: s=s+String.format("SRL A"); break;

      case 0x40: s=s+String.format("BIT 0,B"); break;
      case 0x41: s=s+String.format("BIT 0,C"); break;
      case 0x42: s=s+String.format("BIT 0,D"); break;
      case 0x43: s=s+String.format("BIT 0,E"); break;
      case 0x44: s=s+String.format("BIT 0,H"); break;
      case 0x45: s=s+String.format("BIT 0,L"); break;
      case 0x46: s=s+String.format("BIT 0,(HL)"); break;
      case 0x47: s=s+String.format("BIT 0,A"); break;
      case 0x48: s=s+String.format("BIT 1,B"); break;
      case 0x49: s=s+String.format("BIT 1,C"); break;
      case 0x4A: s=s+String.format("BIT 1,D"); break;
      case 0x4B: s=s+String.format("BIT 1,E"); break;
      case 0x4C: s=s+String.format("BIT 1,H"); break;
      case 0x4D: s=s+String.format("BIT 1,L"); break;
      case 0x4E: s=s+String.format("BIT 1,(HL)"); break;
      case 0x4F: s=s+String.format("BIT 1,A"); break;

      case 0x50: s=s+String.format("BIT 2,B"); break;
      case 0x51: s=s+String.format("BIT 2,C"); break;
      case 0x52: s=s+String.format("BIT 2,D"); break;
      case 0x53: s=s+String.format("BIT 2,E"); break;
      case 0x54: s=s+String.format("BIT 2,H"); break;
      case 0x55: s=s+String.format("BIT 2,L"); break;
      case 0x56: s=s+String.format("BIT 2,(HL)"); break;
      case 0x57: s=s+String.format("BIT 2,A"); break;
      case 0x58: s=s+String.format("BIT 3,B"); break;
      case 0x59: s=s+String.format("BIT 3,C"); break;
      case 0x5A: s=s+String.format("BIT 3,D"); break;
      case 0x5B: s=s+String.format("BIT 3,E"); break;
      case 0x5C: s=s+String.format("BIT 3,H"); break;
      case 0x5D: s=s+String.format("BIT 3,L"); break;
      case 0x5E: s=s+String.format("BIT 3,(HL)"); break;
      case 0x5F: s=s+String.format("BIT 3,A"); break;

      case 0x60: s=s+String.format("BIT 4,B"); break;
      case 0x61: s=s+String.format("BIT 4,C"); break;
      case 0x62: s=s+String.format("BIT 4,D"); break;
      case 0x63: s=s+String.format("BIT 4,E"); break;
      case 0x64: s=s+String.format("BIT 4,H"); break;
      case 0x65: s=s+String.format("BIT 4,L"); break;
      case 0x66: s=s+String.format("BIT 4,(HL)"); break;
      case 0x67: s=s+String.format("BIT 4,A"); break;
      case 0x68: s=s+String.format("BIT 5,B"); break;
      case 0x69: s=s+String.format("BIT 5,C"); break;
      case 0x6A: s=s+String.format("BIT 5,D"); break;
      case 0x6B: s=s+String.format("BIT 5,E"); break;
      case 0x6C: s=s+String.format("BIT 5,H"); break;
      case 0x6D: s=s+String.format("BIT 5,L"); break;
      case 0x6E: s=s+String.format("BIT 5,(HL)"); break;
      case 0x6F: s=s+String.format("BIT 5,A"); break;

      case 0x70: s=s+String.format("BIT 6,B"); break;
      case 0x71: s=s+String.format("BIT 6,C"); break;
      case 0x72: s=s+String.format("BIT 6,D"); break;
      case 0x73: s=s+String.format("BIT 6,E"); break;
      case 0x74: s=s+String.format("BIT 6,H"); break;
      case 0x75: s=s+String.format("BIT 6,L"); break;
      case 0x76: s=s+String.format("BIT 6,(HL)"); break;
      case 0x77: s=s+String.format("BIT 6,A"); break;
      case 0x78: s=s+String.format("BIT 7,B"); break;
      case 0x79: s=s+String.format("BIT 7,C"); break;
      case 0x7A: s=s+String.format("BIT 7,D"); break;
      case 0x7B: s=s+String.format("BIT 7,E"); break;
      case 0x7C: s=s+String.format("BIT 7,H"); break;
      case 0x7D: s=s+String.format("BIT 7,L"); break;
      case 0x7E: s=s+String.format("BIT 7,(HL)"); break;
      case 0x7F: s=s+String.format("BIT 7,A"); break;

      case 0x80: s=s+String.format("RES 0,B"); break;
      case 0x81: s=s+String.format("RES 0,C"); break;
      case 0x82: s=s+String.format("RES 0,D"); break;
      case 0x83: s=s+String.format("RES 0,E"); break;
      case 0x84: s=s+String.format("RES 0,H"); break;
      case 0x85: s=s+String.format("RES 0,L"); break;
      case 0x86: s=s+String.format("RES 0,(HL)"); break;
      case 0x87: s=s+String.format("RES 0,A"); break;
      case 0x88: s=s+String.format("RES 1,B"); break;
      case 0x89: s=s+String.format("RES 1,C"); break;
      case 0x8A: s=s+String.format("RES 1,D"); break;
      case 0x8B: s=s+String.format("RES 1,E"); break;
      case 0x8C: s=s+String.format("RES 1,H"); break;
      case 0x8D: s=s+String.format("RES 1,L"); break;
      case 0x8E: s=s+String.format("RES 1,(HL)"); break;
      case 0x8F: s=s+String.format("RES 1,A"); break;

      case 0x90: s=s+String.format("RES 2,B"); break;
      case 0x91: s=s+String.format("RES 2,C"); break;
      case 0x92: s=s+String.format("RES 2,D"); break;
      case 0x93: s=s+String.format("RES 2,E"); break;
      case 0x94: s=s+String.format("RES 2,H"); break;
      case 0x95: s=s+String.format("RES 2,L"); break;
      case 0x96: s=s+String.format("RES 2,(HL)"); break;
      case 0x97: s=s+String.format("RES 2,A"); break;
      case 0x98: s=s+String.format("RES 3,B"); break;
      case 0x99: s=s+String.format("RES 3,C"); break;
      case 0x9A: s=s+String.format("RES 3,D"); break;
      case 0x9B: s=s+String.format("RES 3,E"); break;
      case 0x9C: s=s+String.format("RES 3,H"); break;
      case 0x9D: s=s+String.format("RES 3,L"); break;
      case 0x9E: s=s+String.format("RES 3,(HL)"); break;
      case 0x9F: s=s+String.format("RES 3,A"); break;

      case 0xA0: s=s+String.format("RES 4,B"); break;
      case 0xA1: s=s+String.format("RES 4,C"); break;
      case 0xA2: s=s+String.format("RES 4,D"); break;
      case 0xA3: s=s+String.format("RES 4,E"); break;
      case 0xA4: s=s+String.format("RES 4,H"); break;
      case 0xA5: s=s+String.format("RES 4,L"); break;
      case 0xA6: s=s+String.format("RES 4,(HL)"); break;
      case 0xA7: s=s+String.format("RES 4,A"); break;
      case 0xA8: s=s+String.format("RES 5,B"); break;
      case 0xA9: s=s+String.format("RES 5,C"); break;
      case 0xAA: s=s+String.format("RES 5,D"); break;
      case 0xAB: s=s+String.format("RES 5,E"); break;
      case 0xAC: s=s+String.format("RES 5,H"); break;
      case 0xAD: s=s+String.format("RES 5,L"); break;
      case 0xAE: s=s+String.format("RES 5,(HL)"); break;
      case 0xAF: s=s+String.format("RES 5,A"); break;

      case 0xB0: s=s+String.format("RES 6,B"); break;
      case 0xB1: s=s+String.format("RES 6,C"); break;
      case 0xB2: s=s+String.format("RES 6,D"); break;
      case 0xB3: s=s+String.format("RES 6,E"); break;
      case 0xB4: s=s+String.format("RES 6,H"); break;
      case 0xB5: s=s+String.format("RES 6,L"); break;
      case 0xB6: s=s+String.format("RES 6,(HL)"); break;
      case 0xB7: s=s+String.format("RES 6,A"); break;
      case 0xB8: s=s+String.format("RES 7,B"); break;
      case 0xB9: s=s+String.format("RES 7,C"); break;
      case 0xBA: s=s+String.format("RES 7,D"); break;
      case 0xBB: s=s+String.format("RES 7,E"); break;
      case 0xBC: s=s+String.format("RES 7,H"); break;
      case 0xBD: s=s+String.format("RES 7,L"); break;
      case 0xBE: s=s+String.format("RES 7,(HL)"); break;
      case 0xBF: s=s+String.format("RES 7,A"); break;

      case 0xC0: s=s+String.format("SET 0,B"); break;
      case 0xC1: s=s+String.format("SET 0,C"); break;
      case 0xC2: s=s+String.format("SET 0,D"); break;
      case 0xC3: s=s+String.format("SET 0,E"); break;
      case 0xC4: s=s+String.format("SET 0,H"); break;
      case 0xC5: s=s+String.format("SET 0,L"); break;
      case 0xC6: s=s+String.format("SET 0,(HL)"); break;
      case 0xC7: s=s+String.format("SET 0,A"); break;
      case 0xC8: s=s+String.format("SET 1,B"); break;
      case 0xC9: s=s+String.format("SET 1,C"); break;
      case 0xCA: s=s+String.format("SET 1,D"); break;
      case 0xCB: s=s+String.format("SET 1,E"); break;
      case 0xCC: s=s+String.format("SET 1,H"); break;
      case 0xCD: s=s+String.format("SET 1,L"); break;
      case 0xCE: s=s+String.format("SET 1,(HL)"); break;
      case 0xCF: s=s+String.format("SET 1,A"); break;

      case 0xD0: s=s+String.format("SET 2,B"); break;
      case 0xD1: s=s+String.format("SET 2,C"); break;
      case 0xD2: s=s+String.format("SET 2,D"); break;
      case 0xD3: s=s+String.format("SET 2,E"); break;
      case 0xD4: s=s+String.format("SET 2,H"); break;
      case 0xD5: s=s+String.format("SET 2,L"); break;
      case 0xD6: s=s+String.format("SET 2,(HL)"); break;
      case 0xD7: s=s+String.format("SET 2,A"); break;
      case 0xD8: s=s+String.format("SET 3,B"); break;
      case 0xD9: s=s+String.format("SET 3,C"); break;
      case 0xDA: s=s+String.format("SET 3,D"); break;
      case 0xDB: s=s+String.format("SET 3,E"); break;
      case 0xDC: s=s+String.format("SET 3,H"); break;
      case 0xDD: s=s+String.format("SET 3,L"); break;
      case 0xDE: s=s+String.format("SET 3,(HL)"); break;
      case 0xDF: s=s+String.format("SET 3,A"); break;

      case 0xE0: s=s+String.format("SET 4,B"); break;
      case 0xE1: s=s+String.format("SET 4,C"); break;
      case 0xE2: s=s+String.format("SET 4,D"); break;
      case 0xE3: s=s+String.format("SET 4,E"); break;
      case 0xE4: s=s+String.format("SET 4,H"); break;
      case 0xE5: s=s+String.format("SET 4,L"); break;
      case 0xE6: s=s+String.format("SET 4,(HL)"); break;
      case 0xE7: s=s+String.format("SET 4,A"); break;
      case 0xE8: s=s+String.format("SET 5,B"); break;
      case 0xE9: s=s+String.format("SET 5,C"); break;
      case 0xEA: s=s+String.format("SET 5,D"); break;
      case 0xEB: s=s+String.format("SET 5,E"); break;
      case 0xEC: s=s+String.format("SET 5,H"); break;
      case 0xED: s=s+String.format("SET 5,L"); break;
      case 0xEE: s=s+String.format("SET 5,(HL)"); break;
      case 0xEF: s=s+String.format("SET 5,A"); break;

      case 0xF0: s=s+String.format("SET 6,B"); break;
      case 0xF1: s=s+String.format("SET 6,C"); break;
      case 0xF2: s=s+String.format("SET 6,D"); break;
      case 0xF3: s=s+String.format("SET 6,E"); break;
      case 0xF4: s=s+String.format("SET 6,H"); break;
      case 0xF5: s=s+String.format("SET 6,L"); break;
      case 0xF6: s=s+String.format("SET 6,(HL)"); break;
      case 0xF7: s=s+String.format("SET 6,A"); break;
      case 0xF8: s=s+String.format("SET 7,B"); break;
      case 0xF9: s=s+String.format("SET 7,C"); break;
      case 0xFA: s=s+String.format("SET 7,D"); break;
      case 0xFB: s=s+String.format("SET 7,E"); break;
      case 0xFC: s=s+String.format("SET 7,H"); break;
      case 0xFD: s=s+String.format("SET 7,L"); break;
      case 0xFE: s=s+String.format("SET 7,(HL)"); break;
      case 0xFF: s=s+String.format("SET 7,A"); break;
      default: s=s+String.format("ILLEGAL INSTRUCTION"); break;
    }
  }
  if(extd_flag!=0){
    inst_bytes=2;
    switch (Opcodes[pc+1]) {
      case 0x23: s=s+String.format("Undocumented 8 T-State NOP"); break;

      case 0x40: s=s+String.format("IN B,(C)"); break;
      case 0x41: s=s+String.format("OUT (C),B"); break;
      case 0x42: s=s+String.format("SBC HL,BC"); break;
      case 0x43: s=s+String.format("LD (#%02X%02X),BC",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x44: s=s+String.format("NEG"); break;
      case 0x45: s=s+String.format("RETN"); break;
      case 0x46: s=s+String.format("IM 0"); break;
      case 0x47: s=s+String.format("LD I,A"); break;
      case 0x48: s=s+String.format("IN C,(C)"); break;
      case 0x49: s=s+String.format("OUT (C),C"); break;
      case 0x4A: s=s+String.format("ADC (HL),BC"); break;
      case 0x4B: s=s+String.format("LD BC,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x4C: s=s+String.format("NEG"); break;
      case 0x4D: s=s+String.format("RETI"); break;
      case 0x4E: s=s+String.format("IM 0/1"); break;
      case 0x4F: s=s+String.format("LD R,A"); break;

      case 0x50: s=s+String.format("IN D,(C)"); break;
      case 0x51: s=s+String.format("OUT (C),D"); break;
      case 0x52: s=s+String.format("SBC HL,DE"); break;
      case 0x53: s=s+String.format("LD (#%02X%02X),DE",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x54: s=s+String.format("NEG"); break;
      case 0x55: s=s+String.format("RETN"); break;
      case 0x56: s=s+String.format("IM 1"); break;
      case 0x57: s=s+String.format("LD A,I"); break;
      case 0x58: s=s+String.format("IN E,(C)"); break;
      case 0x59: s=s+String.format("OUT (C),E"); break;
      case 0x5A: s=s+String.format("ADC (HL),DE"); break;
      case 0x5B: s=s+String.format("LD DE,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x5C: s=s+String.format("NEG"); break;
      case 0x5D: s=s+String.format("RETN"); break;
      case 0x5E: s=s+String.format("IM 2"); break;
      case 0x5F: s=s+String.format("LD A,R"); break;

      case 0x60: s=s+String.format("IN H,(C)"); break;
      case 0x61: s=s+String.format("OUT (C),H"); break;
      case 0x62: s=s+String.format("SBC HL,HL"); break;
      case 0x63: s=s+String.format("LD (#%02X%02X),HL",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x64: s=s+String.format("NEG"); break;
      case 0x65: s=s+String.format("RETN"); break;
      case 0x66: s=s+String.format("IM 0"); break;
      case 0x67: s=s+String.format("RRD"); break;
      case 0x68: s=s+String.format("IN L,(C)"); break;
      case 0x69: s=s+String.format("OUT (C),L"); break;
      case 0x6A: s=s+String.format("ADC (HL),HL"); break;
      case 0x6B: s=s+String.format("LD HL,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x6C: s=s+String.format("NEG"); break;
      case 0x6D: s=s+String.format("RETN"); break;
      case 0x6E: s=s+String.format("IM 0/1"); break;
      case 0x6F: s=s+String.format("RLD"); break;

      case 0x70: s=s+String.format("IN (C)"); break;
      case 0x71: s=s+String.format("OUT (C),0"); break;
      case 0x72: s=s+String.format("SBC HL,SP"); break;
      case 0x73: s=s+String.format("LD (#%02X%02X),SP",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x74: s=s+String.format("NEG"); break;
      case 0x75: s=s+String.format("RETN"); break;
      case 0x76: s=s+String.format("IM 1"); break;

      case 0x78: s=s+String.format("IN A,(C)"); break;
      case 0x79: s=s+String.format("OUT (C),A"); break;
      case 0x7A: s=s+String.format("ADC (HL),SP"); break;
      case 0x7B: s=s+String.format("LD SP,(#%02X%02X)",Opcodes[pc+2],Opcodes[pc+1]); inst_bytes=4; break;
      case 0x7C: s=s+String.format("NEG"); break;
      case 0x7D: s=s+String.format("RETN"); break;
      case 0x7E: s=s+String.format("IM 2"); break;

      case 0xA0: s=s+String.format("LDI"); break;
      case 0xA1: s=s+String.format("CPI"); break;
      case 0xA2: s=s+String.format("INI"); break;
      case 0xA3: s=s+String.format("OUTI"); break;
      case 0xA8: s=s+String.format("LDD"); break;
      case 0xA9: s=s+String.format("CPD"); break;
      case 0xAA: s=s+String.format("IMD"); break;
      case 0xAB: s=s+String.format("OUTD"); break;

      case 0xB0: s=s+String.format("LDIR"); break;
      case 0xB1: s=s+String.format("CPIR"); break;
      case 0xB2: s=s+String.format("INIR"); break;
      case 0xB3: s=s+String.format("OUTIR"); break;
      case 0xB8: s=s+String.format("LDDR"); break;
      case 0xB9: s=s+String.format("CPDR"); break;
      case 0xBA: s=s+String.format("IMDR"); break;
      case 0xBB: s=s+String.format("OUTDR"); break;

      default: s=s+String.format("ILLEGAL INSTRUCTION");break;
    }
  }

  if(ix_flag!=0){
    inst_bytes=2;
    switch (Opcodes[pc+1]) {
      case 0x09:s=s+String.format("ADD IX,BC"); break;
      case 0x19:s=s+String.format("ADD IX,DE"); break;

      case 0x21: s=s+String.format("LD IX,#%02X%02X",Opcodes[pc+3],Opcodes[pc+2]); inst_bytes=4; break;
      case 0x22: s=s+String.format("LD (#%02X%02X),IX",Opcodes[pc+3],Opcodes[pc+2]); inst_bytes=4; break;
      case 0x23: s=s+String.format("INC IX"); break;
      case 0x24: s=s+String.format("INC IXH"); break;
      case 0x25: s=s+String.format("DEC IXH"); break;
      case 0x26: s=s+String.format("LD IXH,#%02X",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x29: s=s+String.format("ADD IX,IX"); break;
      case 0x2A: s=s+String.format("LD IX,(#%02X%02X)",Opcodes[pc+3],Opcodes[pc+2]); inst_bytes=4; break;
      case 0x2B: s=s+String.format("DEC IX"); break;
      case 0x2C: s=s+String.format("INC IXL"); break;
      case 0x2D: s=s+String.format("DEC IXL"); break;
      case 0x2E: s=s+String.format("LD IXL,#%02X",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x34: s=s+String.format("INC (IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x35: s=s+String.format("DEC (IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x36: s=s+String.format("LD (IX+#%02X),#%02X",Opcodes[pc+3],Opcodes[pc+3]); inst_bytes=4; break;
      case 0x39: s=s+String.format("ADD IX,SP"); break;

      case 0x44: s=s+String.format("LD B,IXH"); break;
      case 0x45: s=s+String.format("LD B,IXL"); break;
      case 0x46: s=s+String.format("LD B,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x4C: s=s+String.format("LD C,IXH"); break;
      case 0x4D: s=s+String.format("LD C,IXL"); break;
      case 0x4E: s=s+String.format("LD C,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x54: s=s+String.format("LD D,IXH"); break;
      case 0x55: s=s+String.format("LD D,IXL"); break;
      case 0x56: s=s+String.format("LD D,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x5C: s=s+String.format("LD E,IXH"); break;
      case 0x5D: s=s+String.format("LD E,IXL"); break;
      case 0x5E: s=s+String.format("LD E,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x60: s=s+String.format("LD IXH,B"); break;
      case 0x61: s=s+String.format("LD IXH,C"); break;
      case 0x62: s=s+String.format("LD IXH,D"); break;
      case 0x63: s=s+String.format("LD IXH,E"); break;
      case 0x64: s=s+String.format("LD IXH,IXH"); break;
      case 0x65: s=s+String.format("LD IXH,IXL"); break;
      case 0x66: s=s+String.format("LD H,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x67: s=s+String.format("LD IXH,A"); break;
      case 0x68: s=s+String.format("LD IXL,B"); break;
      case 0x69: s=s+String.format("LD IXL,C"); break;
      case 0x6A: s=s+String.format("LD IXL,D"); break;
      case 0x6B: s=s+String.format("LD IXL,E"); break;
      case 0x6C: s=s+String.format("LD IXL,IXH"); break;
      case 0x6D: s=s+String.format("LD IXL,IXL"); break;
      case 0x6E: s=s+String.format("LD L,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x6F: s=s+String.format("LD IXL,A"); break;

      case 0x70: s=s+String.format("LD (IX+#%02X),B",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x71: s=s+String.format("LD (IX+#%02X),C",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x72: s=s+String.format("LD (IX+#%02X),D",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x73: s=s+String.format("LD (IX+#%02X),E",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x74: s=s+String.format("LD (IX+#%02X),H",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x75: s=s+String.format("LD (IX+#%02X),L",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x77: s=s+String.format("LD (IX+#%02X),A",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x7C: s=s+String.format("LD A,IXH"); break;
      case 0x7D: s=s+String.format("LD A,IXL"); break;
      case 0x7E: s=s+String.format("LD A,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x84: s=s+String.format("ADD A,IXH"); break;
      case 0x85: s=s+String.format("ADD A,IXL"); break;
      case 0x86: s=s+String.format("ADD A,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x8C: s=s+String.format("ADC A,IXH"); break;
      case 0x8D: s=s+String.format("ADC A,IXL"); break;
      case 0x8E: s=s+String.format("ADC A,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x94: s=s+String.format("SUB IXH"); break;
      case 0x95: s=s+String.format("SUB IXL"); break;
      case 0x96: s=s+String.format("SUB (IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x9C: s=s+String.format("SBC A,IXH"); break;
      case 0x9D: s=s+String.format("SBC A,IXL"); break;
      case 0x9E: s=s+String.format("SBC A,(IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0xA4: s=s+String.format("AND IXH"); break;
      case 0xA5: s=s+String.format("AND IXL"); break;
      case 0xA6: s=s+String.format("AND (IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0xAC: s=s+String.format("XOR IXH"); break;
      case 0xAD: s=s+String.format("XOR IXL"); break;
      case 0xAE: s=s+String.format("XOR (IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0xB4: s=s+String.format("OR IXH"); break;
      case 0xB5: s=s+String.format("OR IXL"); break;
      case 0xB6: s=s+String.format("OR (IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0xBC: s=s+String.format("CP IXH"); break;
      case 0xBD: s=s+String.format("CP IXL"); break;
      case 0xBE: s=s+String.format("CP (IX+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0xCB: ix_bit_flag=1; break;

      case 0xE1: s=s+String.format("POP IX"); break;
      case 0xE3: s=s+String.format("EX (SP),IX"); break;
      case 0xE5: s=s+String.format("PUSH IX"); break;
      case 0xE9: s=s+String.format("JP (IX)"); break;

      case 0xF9: s=s+String.format("LD SP,IX"); break;

      default: s=s+String.format("ILLEGAL INSTRUCTION"); break;
    }
  }
  
  if(ix_bit_flag!=0){
    inst_bytes=4;
    switch (Opcodes[pc+3]){
      case 0x00: s=s+String.format("RLC (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x01: s=s+String.format("RLC (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x02: s=s+String.format("RLC (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x03: s=s+String.format("RLC (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x04: s=s+String.format("RLC (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x05: s=s+String.format("RLC (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x06: s=s+String.format("RLC (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x07: s=s+String.format("RLC (IX+#%02X), A",Opcodes[pc+2]); break;
      case 0x08: s=s+String.format("RRC (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x09: s=s+String.format("RRC (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x0A: s=s+String.format("RRC (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x0B: s=s+String.format("RRC (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x0C: s=s+String.format("RRC (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x0D: s=s+String.format("RRC (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x0E: s=s+String.format("RRC (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x0F: s=s+String.format("RRC (IX+#%02X),A",Opcodes[pc+2]); break;

      case 0x10: s=s+String.format("RL (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x11: s=s+String.format("RL (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x12: s=s+String.format("RL (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x13: s=s+String.format("RL (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x14: s=s+String.format("RL (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x15: s=s+String.format("RL (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x16: s=s+String.format("RL (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x17: s=s+String.format("RL (IX+#%02X),A",Opcodes[pc+2]); break;
      case 0x18: s=s+String.format("RR (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x19: s=s+String.format("RR (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x1A: s=s+String.format("RR (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x1B: s=s+String.format("RR (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x1C: s=s+String.format("RR (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x1D: s=s+String.format("RR (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x1E: s=s+String.format("RR (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x1F: s=s+String.format("RR (IX+#%02X),A",Opcodes[pc+2]); break;

      case 0x20: s=s+String.format("SLA (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x21: s=s+String.format("SLA (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x22: s=s+String.format("SLA (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x23: s=s+String.format("SLA (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x24: s=s+String.format("SLA (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x25: s=s+String.format("SLA (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x26: s=s+String.format("SLA (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x27: s=s+String.format("SLA (IX+#%02X),A",Opcodes[pc+2]); break;
      case 0x28: s=s+String.format("SRA (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x29: s=s+String.format("SRA (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x2A: s=s+String.format("SRA (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x2B: s=s+String.format("SRA (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x2C: s=s+String.format("SRA (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x2D: s=s+String.format("SRA (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x2E: s=s+String.format("SRA (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x2F: s=s+String.format("SRA (IX+#%02X),A",Opcodes[pc+2]); break;

      case 0x30: s=s+String.format("SLL (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x31: s=s+String.format("SLL (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x32: s=s+String.format("SLL (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x33: s=s+String.format("SLL (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x34: s=s+String.format("SLL (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x35: s=s+String.format("SLL (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x36: s=s+String.format("SLL (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x37: s=s+String.format("SLA (IX+#%02X),A",Opcodes[pc+2]); break;
      case 0x38: s=s+String.format("SRL (IX+#%02X),B",Opcodes[pc+2]); break;
      case 0x39: s=s+String.format("SRL (IX+#%02X),C",Opcodes[pc+2]); break;
      case 0x3A: s=s+String.format("SRL (IX+#%02X),D",Opcodes[pc+2]); break;
      case 0x3B: s=s+String.format("SRL (IX+#%02X),E",Opcodes[pc+2]); break;
      case 0x3C: s=s+String.format("SRL (IX+#%02X),H",Opcodes[pc+2]); break;
      case 0x3D: s=s+String.format("SRL (IX+#%02X),L",Opcodes[pc+2]); break;
      case 0x3E: s=s+String.format("SRL (IX+#%02X)",Opcodes[pc+2]); break;
      case 0x3F: s=s+String.format("SRL (IX+#%02X),A",Opcodes[pc+2]); break;

      case 0x40: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x41: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x42: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x43: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x44: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x45: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x46: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x47: s=s+String.format("BIT 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x48: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x49: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x4A: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x4B: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x4C: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x4D: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x4E: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x4F: s=s+String.format("BIT 1,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0x50: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x51: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x52: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x53: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x54: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x55: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x56: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x57: s=s+String.format("BIT 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x58: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x59: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x5A: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x5B: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x5C: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x5D: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x5E: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x5F: s=s+String.format("BIT 3,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0x60: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x61: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x62: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x63: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x64: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x65: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x66: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x67: s=s+String.format("BIT 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x68: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x69: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x6A: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x6B: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x6C: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x6D: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x6E: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x6F: s=s+String.format("BIT 5,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0x70: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x71: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x72: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x73: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x74: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x75: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x76: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x77: s=s+String.format("BIT 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x78: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x79: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x7A: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x7B: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x7C: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x7D: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x7E: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x7F: s=s+String.format("BIT 7,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0x80: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x81: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x82: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x83: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x84: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x85: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x86: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x87: s=s+String.format("RES 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x88: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x89: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x8A: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x8B: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x8C: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x8D: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x8E: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x8F: s=s+String.format("RES 1,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0x90: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x91: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x92: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x93: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x94: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x95: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x96: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x97: s=s+String.format("RES 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x98: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x99: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x9A: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x9B: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x9C: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x9D: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x9E: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0x9F: s=s+String.format("RES 3,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0xA0: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA1: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA2: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA3: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA4: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA5: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA6: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA7: s=s+String.format("RES 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA8: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xA9: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xAA: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xAB: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xAC: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xAD: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xAE: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xAF: s=s+String.format("RES 5,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0xB0: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB1: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB2: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB3: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB4: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB5: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB6: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB7: s=s+String.format("RES 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB8: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xB9: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xBA: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xBB: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xBC: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xBD: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xBE: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xBF: s=s+String.format("RES 7,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0xC0: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC1: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC2: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC3: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC4: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC5: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC6: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC7: s=s+String.format("SET 0,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC8: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xC9: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xCA: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xCB: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xCC: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xCD: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xCE: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xCF: s=s+String.format("SET 1,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0xD0: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD1: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD2: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD3: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD4: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD5: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD6: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD7: s=s+String.format("SET 2,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD8: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xD9: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xDA: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xDB: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xDC: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xDD: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xDE: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xDF: s=s+String.format("SET 3,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0xE0: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE1: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE2: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE3: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE4: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE5: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE6: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE7: s=s+String.format("SET 4,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE8: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xE9: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xEA: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xEB: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xEC: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xED: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xEE: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xEF: s=s+String.format("SET 5,(IX+#%02X)",Opcodes[pc+2]); break;

      case 0xF0: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF1: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF2: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF3: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF4: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF5: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF6: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF7: s=s+String.format("SET 6,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF8: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xF9: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xFA: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xFB: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xFC: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xFD: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xFE: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      case 0xFF: s=s+String.format("SET 7,(IX+#%02X)",Opcodes[pc+2]); break;
      default: s=s+String.format("ILLEGAL INSTRUCTION"); break;
    }
  }
  
  if(iy_flag!=0){
    inst_bytes=2;
    switch (Opcodes[pc+1]) {
      case 0x09:s=s+String.format("ADD IY,BC"); break;
      case 0x19:s=s+String.format("ADD IY,DE"); break;

      case 0x21: s=s+String.format("LD IY,#%02X%02X",Opcodes[pc+3],Opcodes[pc+2]); inst_bytes=4; break;
      case 0x22: s=s+String.format("LD (#%02X%02X),IY",Opcodes[pc+3],Opcodes[pc+2]); inst_bytes=4; break;
      case 0x23: s=s+String.format("INC IY"); break;
      case 0x24: s=s+String.format("INC IYH"); break;
      case 0x25: s=s+String.format("DEC IYH"); break;
      case 0x26: s=s+String.format("LD IYH,#%02X",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x29: s=s+String.format("ADD IY,IY"); break;
      case 0x2A: s=s+String.format("LD IY,(#%02X%02X)",Opcodes[pc+3],Opcodes[pc+2]); inst_bytes=4; break;
      case 0x2B: s=s+String.format("DEC IY"); break;
      case 0x2C: s=s+String.format("INC IYL"); break;
      case 0x2D: s=s+String.format("DEC IYL"); break;
      case 0x2E: s=s+String.format("LD IYL,#%02X",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x34: s=s+String.format("INC (IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x35: s=s+String.format("DEC (IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x36: s=s+String.format("LD (IY+#%02X),#%02X",Opcodes[pc+3],Opcodes[pc+3]); inst_bytes=4; break;
      case 0x39: s=s+String.format("ADD IY,SP"); break;

      case 0x44: s=s+String.format("LD B,IYH"); break;
      case 0x45: s=s+String.format("LD B,IYL"); break;
      case 0x46: s=s+String.format("LD B,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x4C: s=s+String.format("LD C,IYH"); break;
      case 0x4D: s=s+String.format("LD C,IYL"); break;
      case 0x4E: s=s+String.format("LD C,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x54: s=s+String.format("LD D,IYH"); break;
      case 0x55: s=s+String.format("LD D,IYL"); break;
      case 0x56: s=s+String.format("LD D,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x5C: s=s+String.format("LD E,IYH"); break;
      case 0x5D: s=s+String.format("LD E,IYL"); break;
      case 0x5E: s=s+String.format("LD E,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x60: s=s+String.format("LD IYH,B"); break;
      case 0x61: s=s+String.format("LD IYH,C"); break;
      case 0x62: s=s+String.format("LD IYH,D"); break;
      case 0x63: s=s+String.format("LD IYH,E"); break;
      case 0x64: s=s+String.format("LD IYH,IYH"); break;
      case 0x65: s=s+String.format("LD IYH,IYL"); break;
      case 0x66: s=s+String.format("LD H,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x67: s=s+String.format("LD IYH,A"); break;
      case 0x68: s=s+String.format("LD IYL,B"); break;
      case 0x69: s=s+String.format("LD IYL,C"); break;
      case 0x6A: s=s+String.format("LD IYL,D"); break;
      case 0x6B: s=s+String.format("LD IYL,E"); break;
      case 0x6C: s=s+String.format("LD IYL,IYH"); break;
      case 0x6D: s=s+String.format("LD IYL,IYL"); break;
      case 0x6E: s=s+String.format("LD L,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x6F: s=s+String.format("LD IYL,A"); break;

      case 0x70: s=s+String.format("LD (IY+#%02X),B",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x71: s=s+String.format("LD (IY+#%02X),C",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x72: s=s+String.format("LD (IY+#%02X),D",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x73: s=s+String.format("LD (IY+#%02X),E",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x74: s=s+String.format("LD (IY+#%02X),H",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x75: s=s+String.format("LD (IY+#%02X),L",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x77: s=s+String.format("LD (IY+#%02X),A",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x7C: s=s+String.format("LD A,IYH"); break;
      case 0x7D: s=s+String.format("LD A,IYL"); break;
      case 0x7E: s=s+String.format("LD A,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x84: s=s+String.format("ADD A,IYH"); break;
      case 0x85: s=s+String.format("ADD A,IYL"); break;
      case 0x86: s=s+String.format("ADD A,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x8C: s=s+String.format("ADC A,IYH"); break;
      case 0x8D: s=s+String.format("ADC A,IYL"); break;
      case 0x8E: s=s+String.format("ADC A,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0x94: s=s+String.format("SUB IYH"); break;
      case 0x95: s=s+String.format("SUB IYL"); break;
      case 0x96: s=s+String.format("SUB (IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0x9C: s=s+String.format("SBC A,IYH"); break;
      case 0x9D: s=s+String.format("SBC A,IYL"); break;
      case 0x9E: s=s+String.format("SBC A,(IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0xA4: s=s+String.format("AND IYH"); break;
      case 0xA5: s=s+String.format("AND IYL"); break;
      case 0xA6: s=s+String.format("AND (IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0xAC: s=s+String.format("XOR IYH"); break;
      case 0xAD: s=s+String.format("XOR IYL"); break;
      case 0xAE: s=s+String.format("XOR (IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0xB4: s=s+String.format("OR IYH"); break;
      case 0xB5: s=s+String.format("OR IYL"); break;
      case 0xB6: s=s+String.format("OR (IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;
      case 0xBC: s=s+String.format("CP IYH"); break;
      case 0xBD: s=s+String.format("CP IYL"); break;
      case 0xBE: s=s+String.format("CP (IY+#%02X)",Opcodes[pc+2]); inst_bytes=3; break;

      case 0xCB: iy_bit_flag=1; break;

      case 0xE1: s=s+String.format("POP IY"); break;
      case 0xE3: s=s+String.format("EX (SP),IY"); break;
      case 0xE5: s=s+String.format("PUSH IY"); break;
      case 0xE9: s=s+String.format("JP (IY)"); break;

      case 0xF9: s=s+String.format("LD SP,IY"); break;

      default: s=s+String.format("ILLEGAL INSTRUCTION"); break;
    }

  }
  if(iy_bit_flag!=0){
    inst_bytes=4;
    switch (Opcodes[pc+3]){
      case 0x00: s=s+String.format("RLC (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x01: s=s+String.format("RLC (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x02: s=s+String.format("RLC (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x03: s=s+String.format("RLC (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x04: s=s+String.format("RLC (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x05: s=s+String.format("RLC (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x06: s=s+String.format("RLC (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x07: s=s+String.format("RLC (IY+#%02X), A",Opcodes[pc+2]); break;
      case 0x08: s=s+String.format("RRC (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x09: s=s+String.format("RRC (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x0A: s=s+String.format("RRC (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x0B: s=s+String.format("RRC (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x0C: s=s+String.format("RRC (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x0D: s=s+String.format("RRC (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x0E: s=s+String.format("RRC (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x0F: s=s+String.format("RRC (IY+#%02X),A",Opcodes[pc+2]); break;

      case 0x10: s=s+String.format("RL (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x11: s=s+String.format("RL (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x12: s=s+String.format("RL (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x13: s=s+String.format("RL (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x14: s=s+String.format("RL (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x15: s=s+String.format("RL (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x16: s=s+String.format("RL (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x17: s=s+String.format("RL (IY+#%02X),A",Opcodes[pc+2]); break;
      case 0x18: s=s+String.format("RR (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x19: s=s+String.format("RR (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x1A: s=s+String.format("RR (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x1B: s=s+String.format("RR (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x1C: s=s+String.format("RR (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x1D: s=s+String.format("RR (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x1E: s=s+String.format("RR (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x1F: s=s+String.format("RR (IY+#%02X),A",Opcodes[pc+2]); break;

      case 0x20: s=s+String.format("SLA (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x21: s=s+String.format("SLA (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x22: s=s+String.format("SLA (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x23: s=s+String.format("SLA (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x24: s=s+String.format("SLA (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x25: s=s+String.format("SLA (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x26: s=s+String.format("SLA (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x27: s=s+String.format("SLA (IY+#%02X),A",Opcodes[pc+2]); break;
      case 0x28: s=s+String.format("SRA (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x29: s=s+String.format("SRA (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x2A: s=s+String.format("SRA (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x2B: s=s+String.format("SRA (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x2C: s=s+String.format("SRA (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x2D: s=s+String.format("SRA (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x2E: s=s+String.format("SRA (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x2F: s=s+String.format("SRA (IY+#%02X),A",Opcodes[pc+2]); break;

      case 0x30: s=s+String.format("SLL (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x31: s=s+String.format("SLL (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x32: s=s+String.format("SLL (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x33: s=s+String.format("SLL (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x34: s=s+String.format("SLL (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x35: s=s+String.format("SLL (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x36: s=s+String.format("SLL (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x37: s=s+String.format("SLA (IY+#%02X),A",Opcodes[pc+2]); break;
      case 0x38: s=s+String.format("SRL (IY+#%02X),B",Opcodes[pc+2]); break;
      case 0x39: s=s+String.format("SRL (IY+#%02X),C",Opcodes[pc+2]); break;
      case 0x3A: s=s+String.format("SRL (IY+#%02X),D",Opcodes[pc+2]); break;
      case 0x3B: s=s+String.format("SRL (IY+#%02X),E",Opcodes[pc+2]); break;
      case 0x3C: s=s+String.format("SRL (IY+#%02X),H",Opcodes[pc+2]); break;
      case 0x3D: s=s+String.format("SRL (IY+#%02X),L",Opcodes[pc+2]); break;
      case 0x3E: s=s+String.format("SRL (IY+#%02X)",Opcodes[pc+2]); break;
      case 0x3F: s=s+String.format("SRL (IY+#%02X),A",Opcodes[pc+2]); break;

      case 0x40: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x41: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x42: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x43: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x44: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x45: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x46: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x47: s=s+String.format("BIT 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x48: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x49: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x4A: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x4B: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x4C: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x4D: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x4E: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x4F: s=s+String.format("BIT 1,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0x50: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x51: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x52: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x53: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x54: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x55: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x56: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x57: s=s+String.format("BIT 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x58: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x59: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x5A: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x5B: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x5C: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x5D: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x5E: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x5F: s=s+String.format("BIT 3,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0x60: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x61: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x62: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x63: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x64: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x65: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x66: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x67: s=s+String.format("BIT 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x68: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x69: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x6A: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x6B: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x6C: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x6D: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x6E: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x6F: s=s+String.format("BIT 5,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0x70: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x71: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x72: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x73: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x74: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x75: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x76: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x77: s=s+String.format("BIT 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x78: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x79: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x7A: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x7B: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x7C: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x7D: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x7E: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x7F: s=s+String.format("BIT 7,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0x80: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x81: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x82: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x83: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x84: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x85: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x86: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x87: s=s+String.format("RES 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x88: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x89: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x8A: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x8B: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x8C: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x8D: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x8E: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x8F: s=s+String.format("RES 1,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0x90: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x91: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x92: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x93: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x94: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x95: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x96: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x97: s=s+String.format("RES 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x98: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x99: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x9A: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x9B: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x9C: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x9D: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x9E: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0x9F: s=s+String.format("RES 3,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0xA0: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA1: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA2: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA3: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA4: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA5: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA6: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA7: s=s+String.format("RES 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA8: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xA9: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xAA: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xAB: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xAC: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xAD: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xAE: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xAF: s=s+String.format("RES 5,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0xB0: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB1: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB2: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB3: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB4: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB5: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB6: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB7: s=s+String.format("RES 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB8: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xB9: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xBA: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xBB: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xBC: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xBD: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xBE: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xBF: s=s+String.format("RES 7,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0xC0: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC1: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC2: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC3: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC4: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC5: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC6: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC7: s=s+String.format("SET 0,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC8: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xC9: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xCA: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xCB: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xCC: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xCD: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xCE: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xCF: s=s+String.format("SET 1,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0xD0: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD1: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD2: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD3: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD4: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD5: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD6: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD7: s=s+String.format("SET 2,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD8: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xD9: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xDA: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xDB: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xDC: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xDD: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xDE: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xDF: s=s+String.format("SET 3,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0xE0: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE1: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE2: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE3: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE4: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE5: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE6: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE7: s=s+String.format("SET 4,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE8: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xE9: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xEA: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xEB: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xEC: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xED: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xEE: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xEF: s=s+String.format("SET 5,(IY+#%02X)",Opcodes[pc+2]); break;

      case 0xF0: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF1: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF2: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF3: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF4: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF5: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF6: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF7: s=s+String.format("SET 6,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF8: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xF9: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xFA: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xFB: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xFC: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xFD: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xFE: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      case 0xFF: s=s+String.format("SET 7,(IY+#%02X)",Opcodes[pc+2]); break;
      default: s=s+String.format("ILLEGAL INSTRUCTION"); break;
    }
  }

  return s;
 }

}

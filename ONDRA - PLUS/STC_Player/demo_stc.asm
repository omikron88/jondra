;**********************************************************
; Minimal STC player demo for OndraMELODIK
; © 2024 Jan Herman/72ka
; 2hp@seznam.cz
;**********************************************************

;program start
pstart  .equ #4000
fstart  .equ pstart - 5

;HW defs Ondra related
LS374	.equ 11111101b		; printer port	      			(PORT9)
LS173	.equ 11111110b		; LED, strobe and serial_out	(PORT10)

;program header (SSM ROM)
 	org fstart
	db	01h
	dw	pstart
	dw	length
	
	org pstart		;beginning
	ld a, 01bh ;Esc
	rst 8
	db 0
	ld a, "F" ;FAST
	rst 8
	db 0
	
	call music_init ;init music
	;neverending loop	
.loop
	halt ;wait for int
	call music_play
	jr .loop

;include the player
	include "lib_melodik_STC_player.asm"
;include the STC music file
songdata:
	;incbin "songs/qjeta.stc"
	incbin "songs/dizzyX.stc"	
lnchr:
	db	02h
	dw	pstart
; addresses calc
length	.equ lnchr - pstart

	END

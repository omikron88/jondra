;***********************************************************************
;* UTILITA PRO NASTAVENI MAGNETOFONU pro TESLA ONDRA SPO 186           *
;* Jan Herman (2hp@seznam.cz), 2024                                    *
;***********************************************************************


; pstart je zacatek programu

pstart  equ 4000h
fstart  equ pstart - 5
LS174	equ 11110111b		;PORT3

; zahlavi bloku dat
 	org fstart
	db	01h
	dw	pstart
	dw	length
; zacatek programu
	org pstart
	ld	a, 01b
	out (LS174), a
mgftune:
	;smazat obrazovku a nastavit zobrazeni 120 radku
	;pomoci ViLi sluzeb, tento program je vyhradne pro OndraSD
	ld a, 01bh
	rst 010h
	ld a, "I"
	rst 010h
	ld a, 01fh
	rst 010h
	ld a, 01bh
	rst 010h
	ld a, "L"
	rst 010h
	ld a, 120
	rst 010h
	call 022e2h ;TPON - zapnuti magnetofonu
	halt
	di
.celeznova
	ld b, 60 ;tolikrat opakovat
.znovasloupec
	ld d, b
	push bc
	ld b, 50 ;50 vzroku na jednu sadu - prumerovani
.znovavzorek
	push bc
.znova
	push bc
	;namapuji port, pristrankuje porty do hornich 16kb
	ld	a, 101b
	out	(LS174), a
	ld c,a
	ld b,0
	;pockam na prvni hranu od ktere budu merit
.mdread1
	ld a,(0E00Fh) ;ctu napriklad na adrese E00Fh
	xor c ;zmena?
	jp pe, .skip1
	ld c,a
	jr .mdread1
.skip1
	ld c,a
	inc b
	jr .mdread
.mdread ;odtud uz merim cas do b k dalsi hrane
	ld a,(0E00Fh) ;ctu napriklad na adrese E00Fh - vstup BUSY je spolecny
	xor c ;zmena?
	jp pe, .skip
	ld c,a
	inc b
	jr .mdread
.skip	
	; obnov mapovani
	ld	a, 01b
	out (LS174), a	
	ld a, d
	cpl ;inverze protoze odecitame od nuly a b jede od hora do nuly
	ld c,a ;uschovame do c
	ld a, 0ffh
	sub c ;odecteme pocet predchozich pruchodu, v d je pocet cyklu
	sub 128
	ld l, a
	;potlacit extremne kratke i dlouhe impulsy zbytecne je zobrazovat
	ld a,b
	cp 3
	jp c, .nakresleno
	cp 30
	jp nc, .nakresleno
	ld a, 0fah
	sub b
	ld h,a ;jak daleko doprava bajt nakreslim
.nenipuls
	ld a,(hl)
	;nacitam nahodnou hodnotu, aby to vypadalo jako sum, spis pro efekt
	ld a,r
	or (hl) ;prictu k jiz zobrazenemu vzorku
	ld (hl),a ;a vykreslim na misto kde vyska odpovida dobe mezi hranami pulsu
	jr .bylimpuls
.nakresleno
	;vykresli malou sipicku vlevo co jezdi dolu
	ld h, 0fah
	ld a, 00001000b
	ld (hl),a
	ld a, 00011100b
	inc l
	ld (hl),a
	inc l
	xor a
	ld (hl),a
	dec l
	dec l
.bylimpuls
	pop bc
	djnz .znova
	pop bc
	djnz .znovavzorek
	pop bc
	ld d,b
	djnz .znovasloupec
	;vymaz obrazovku
	ld a, 01fh
	rst 010h
	jp .celeznova
	ret
lnchr:	
	db	02h
	dw	pstart
; vypocet adres
length  equ 	lnchr - pstart
	end



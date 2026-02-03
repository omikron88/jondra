;***************************************************************
; Sound Tracker 1.x compiled song player © 1990 Bzyk/Pentagram
; refactored, optimized and improved by © 2020 mborik/SinDiKat
; modified for OndraMELODIK (SN76489) by © 2024 Jan Herman/72ka
;***************************************************************

@HEADLEN = 27	; file header length
@SAMPLEN = 99	; single sample length
@ORNALEN = 33	; single ornament length
@PATTLEN = 7	; single pattern record length
@NULL = 0

; bit.0 if you want to play without looping
; bit.1 if you play looped but fadeout after few seconds
; bit.7 is set each time when loop or fadeout was passed
; ...or simplified:	2 - fadeout after loop
;			1 - no loop
;			0 - loop forever
music_setup:	db	0

;------------------------------------------------------------------------------
music_init:
		ld	hl,songdata
music_init0:	
		ld	a,(hl)
		ld	(speed),a
		ld	(module_addr+1),hl
		inc	hl
		call	add_offset
		ld	a,(de)
		inc	de
		inc	a
		ld	(song_length),a
		ld	(positions),de
		call	add_offset
		ld	(ornaments),de
		push	de
		call	add_offset
		ld	(patterns),de
		ld	hl,HEADLEN
		call	module_addr
		ex	de,hl
		ld	(samples),hl
		ld	hl,empty_pattern
		ld	(chn1_pattptr),hl
		inc	hl
		ld	de,CHN1+1
		ld	bc,VARSLEN
		ld	(hl),b
		ldir
		pop	hl
		ld	bc,ORNALEN
		xor	a
		call	scan_until
		ld	(apply_volenv+1),a
		ld	(chkfadeout+1),a
		dec	a
		ld	(CHN1+CHP.SmpCnt),a
		ld	(CHN2+CHP.SmpCnt),a
		ld	(CHN3+CHP.SmpCnt),a
		ld	a,1
		ld	(tempo_cnt),a
		inc	hl
		ld	(CHN1+CHP.OrnPtr),hl
		ld	(CHN2+CHP.OrnPtr),hl
		ld	(CHN3+CHP.OrnPtr),hl
		ld	hl,music_setup
		res	7,(hl)
		jp	music_mute

scan_until:	
		cp	(hl)
		ret	z
		add	hl,bc
		jr	scan_until

add_offset:	
		ld	e,(hl)
		inc	hl
		ld	d,(hl)
		inc	hl
		ex	de,hl
		
module_addr:	
		ld	bc,NULL
		add	hl,bc
		ex	de,hl
		ret

new_position:	
		ld	a,(current_pos)
		ld	c,a
		ld	hl,song_length
		cp	(hl)
		jr	c,.continue
		ld	hl,music_setup
		bit	1,(hl)
		call	nz,enablefade
		bit	0,(hl)
		jp	nz,noloop
		xor	a
		ld	c,a
		
.continue:	
		inc	a
		ld	(current_pos),a
		ld	l,c
		ld	h,0
		add	hl,hl
		ld	de,(positions)
		add	hl,de
		ld	c,(hl)
		inc	hl
		ld	a,(hl)
		ld	(get_frequency+1),a
		ld	a,c
		ld	hl,(patterns)
		ld	bc,PATTLEN
		call	scan_until
		inc	hl
		call	add_offset
		ld	(chn1_pattptr),de
		call	add_offset
		ld	(chn2_pattptr),de
		call	add_offset
		ld	(chn3_pattptr),de
		ret

adv_patt_step:	
		dec	(ix+CHP.PatStpCnt)
		ret	p
		ld	a,(ix+CHP.PatStep)
		ld	(ix+CHP.PatStpCnt),a
		ret

;------------------------------------------------------------------------------
music_play:	
		ld	a,(tempo_cnt)
		dec	a
		ld	(tempo_cnt),a
		jr	nz,playback
		ld	a,(speed)
		ld	(tempo_cnt),a

.chn1:		
		ld	ix,CHN1
		call	adv_patt_step
		jp	p,.chn2
		ld	hl,(chn1_pattptr)
		ld	a,(hl)
		inc	a
		call	z,new_position
		ld	hl,(chn1_pattptr)
		call	read_pattern
		ld	(chn1_pattptr),hl

.chn2:		
		ld	ix,CHN2
		call	adv_patt_step
		jp	p,.chn3
		ld	hl,(chn2_pattptr)
		call	read_pattern
		ld	(chn2_pattptr),hl

.chn3:		
		ld	ix,CHN3
		call	adv_patt_step
		jp	p,playback
		ld	hl,(chn3_pattptr)
		call	read_pattern
		ld	(chn3_pattptr),hl

;------------------------------------------------------------------------------
playback:	
		call	chkfadeout
		ld	ix,CHN1
		call	adv_sample
		ld	a,c
		ld	(sample_index+1),a
		call	read_sample
		ld	a,c
		or	b
		rrca
		ld	(AYREGS+AR.Mixer),a
		ld	a,(ix+CHP.SmpCnt)
		inc	a
		jr	z,.chn2
		call	set_noise
		call	get_tone
		ld	(AYREGS+AR.TonA),hl

.chn2:		
		ld	hl,AYREGS+AR.AmplA
		call	apply_volenv
		ld	ix,CHN2
		call	adv_sample
		ld	a,(ix+CHP.SmpCnt)
		inc	a
		jr	z,.chn3
		ld	a,c
		ld	(sample_index+1),a
		call	read_sample
		ld	a,(AYREGS+AR.Mixer)
		or	c
		or	b
		ld	(AYREGS+AR.Mixer),a
		call	set_noise
		call	get_tone
		ld	(AYREGS+AR.TonB),hl

.chn3:		
		ld	hl,AYREGS+AR.AmplB
		call	apply_volenv
		ld	ix,CHN3
		call	adv_sample
		ld	a,(ix+CHP.SmpCnt)
		inc	a
		jr	z,.finish
		ld	a,c
		ld	(sample_index+1),a
		call	read_sample
		ld	a,(AYREGS+AR.Mixer)
		rlc	c
		rlc	b
		or	b
		or	c
		ld	(AYREGS+AR.Mixer),a
		call	set_noise
		call	get_tone
		ld	(AYREGS+AR.TonC),hl

.finish:	
		ld	hl,AYREGS+AR.AmplC
		call	apply_volenv

outx1:	
		;write CH1 Tone
		ld d,10000000b
		ld	hl,AYREGS+AR.TonA
		call sn76489_write_frequency
		;write CH1 ATT
		ld	hl,AYREGS+AR.AmplA
		ld d,10010000b
		call sn76489_write_volume
		
		;write CH2 Tone
		ld d,10100000b
		ld	hl,AYREGS+AR.TonB
		call sn76489_write_frequency
		;write CH2 ATT
		ld	hl,AYREGS+AR.AmplB
		ld d,10110000b
		call sn76489_write_volume

		;write CH3 Tone
		ld d,11000000b
		ld	hl,AYREGS+AR.TonC
		call sn76489_write_frequency
		;write CH3 ATT		
		ld	hl,AYREGS+AR.AmplC
		ld d,11010000b
		call sn76489_write_volume
		
.doNoise		

		;write Noise ATT
		ld a,(AYREGS+AR.Mixer)
		bit 3,a
		jr z, .attfrom1
		bit 4,a
		jr z, .attfrom2
		bit 5,a
		jr z, .attfrom3
		ld hl, mute_volume
		jr .applynoiseatt
.attfrom1	
		ld	hl,AYREGS+AR.AmplA
		jr .applynoiseatt
.attfrom2	
		ld	hl,AYREGS+AR.AmplB
		jr .applynoiseatt
.attfrom3	
		ld	hl,AYREGS+AR.AmplC
.applynoiseatt
		ld d,11110000b
		call sn76489_write_volume	
.endch
		ret

music_mute:	
		ld	hl,AYREGS
		ld	de,AYREGS+1
		ld	bc,AR.Mixer
		ld	(hl),b
		ldir
		jp	outx1

c_note:		
		ld	(ix+CHP.Note),a
		ld	(ix+CHP.SmpIndex),0
		ld	(ix+CHP.SmpCnt),#20
c_empty:	inc	hl
		ret

c_sample:	
		sub	#60
		push	hl
		ld	bc,SAMPLEN
		ld	hl,(samples)
		call	scan_until
		inc	hl
		ld	(ix+CHP.SmpPtr),l
		ld	(ix+CHP.SmpPtr+1),h
		pop	hl
		inc	hl
		jr	read_pattern

c_rest:		
		inc	hl
c_norept:	
		ld	(ix+CHP.SmpCnt),-1
		ret

read_pattern:	
		ld	a,(hl)
		cp	#60		; note
		jr	c,c_note
		cp	#70		; sample
		jr	c,c_sample
		cp	#80		; ornament
		jr	c,c_ornament
		jr	z,c_rest	; rest
		cp	#81		; empty
		jr	z,c_empty
		cp	#82		; ornament off
		jr	z,c_ornoff
		sub	#A1		; speed
		ld	(ix+CHP.PatStpCnt),a
		ld	(ix+CHP.PatStep),a
		inc	hl
		jr	read_pattern

c_ornoff:	
		xor	a
		db	1 ; ld bc,* (skip next instruction)
c_ornament:	
		sub	#70
c_ornament0:	
		push	hl
		ld	bc,ORNALEN
		ld	hl,(ornaments)
		call	scan_until
		inc	hl
		ld	(ix+CHP.OrnPtr),l
		ld	(ix+CHP.OrnPtr+1),h
		ld	(ix+CHP.EnvState),0
		pop	hl
		inc	hl
		jr	read_pattern

adv_sample:	ld	a,(ix+CHP.SmpCnt)
		inc	a
		ret	z
		dec	a
		dec	a
		ld	(ix+CHP.SmpCnt),a
		push	af
		ld	a,(ix+CHP.SmpIndex)
		ld	c,a
		inc	a
		and	#1F
		ld	(ix+CHP.SmpIndex),a
		pop	af
		ret	nz
		ld	e,(ix+CHP.SmpPtr)
		ld	d,(ix+CHP.SmpPtr+1)
		ld	hl,#60	; offset to sample metadata
		add	hl,de
		ld	a,(hl)
		dec	a
		jp	m,c_norept
		ld	c,a	; repeat sample
		inc	a
		and	#1F
		ld	(ix+CHP.SmpIndex),a
		inc	hl
		ld	a,(hl)
		inc	a
		ld	(ix+CHP.SmpCnt),a
		ret

read_sample:	
		ld	d,0
		ld	e,a
		add	a,a
		add	a,e
		ld	e,a
		ld	l,(ix+CHP.SmpPtr)
		ld	h,(ix+CHP.SmpPtr+1)
		add	hl,de
		inc	hl
		ld	a,(hl)
		bit	7,a
		ld	c,#10
		jr	nz,.no_noise
		ld	c,d
.no_noise:	
		bit	6,a
		ld	b,2
		jr	nz,.no_tone
		ld	b,d
.no_tone:	
		inc	hl
		ld	e,(hl)
		dec	hl
		dec	hl
		ld	d,(hl)
		ld	l,a
		and	#1F
		ld	h,a
		ld	a,d
		push	af
		and	#F0
		rrca
		rrca
		rrca
		rrca
		ld	d,a
		pop	af
		and	#0F
		bit	5,l
		ld	l,a
		ret	z
		set	4,d
		ret

set_noise:
		ld	a,c
		or	a
		ret	nz
		ld	a,h
		push de
		ld d,11100000b
		call sn76489_write_noise
		pop de
		ret

apply_volenv:
		ld	(hl),a		; if value of attenuation is higher
		bit	3,a			; then 8 we will stop also envelopes...
		ret	z
		ld	a,(ix+CHP.SmpCnt)
		inc	a
		ret

get_tone:	
		ld	a,l
		push	af
		push	de
		ld	l,(ix+CHP.OrnPtr)
		ld	h,(ix+CHP.OrnPtr+1)
sample_index:	
		ld	de,NULL
		add	hl,de
		ld	a,(ix+CHP.Note)
		add	a,(hl)

get_frequency:	
		add	a,0
		add	a,a
		ld	e,a
		ld	d,0
		ld	hl,freqtable
		add	hl,de
		ld	e,(hl)
		inc	hl
		ld	d,(hl)
		ex	de,hl
		pop	de
		pop	af
		bit	4,d
		jr	z,.negative
		add	hl,de
		ret

.negative:	
		and	a
		sbc	hl,de
		ret

resetfadeout:	
		ld	hl,music_setup
noloop:		
		set	7,(hl)
		call	music_mute
deadend:	
		pop	hl
		xor	a
		ld	(tempo_cnt),a
		dec	a
		ld	(CHN1+CHP.SmpCnt),a
		ld	(CHN2+CHP.SmpCnt),a
		ld	(CHN3+CHP.SmpCnt),a
		ld	(chkfadeout+1),a
		ret

enablefade:	
		ld	a,48
forcefade:	
		ld	(chkfadeout+1),a
		ld	(countfadeout+1),a
		ld	(divfadeout+1),a

chkfadeout:
		xor a
		or	a
		ret	z
		ld	a,(music_setup)
		rlca
		jr	c,deadend
countfadeout:
		xor a
		dec	a
		ld	(countfadeout+1),a
		ret	nz
		ld	a,(apply_volenv+1)
		inc	a
		cp	16
		jr	z,resetfadeout
		ld	(apply_volenv+1),a
divfadeout:	
		xor a
		srl	a
		ld	e,a
		srl	e
		add	a,e
		jr	nz,divfadeout1
		inc	a
divfadeout1:	
		ld	(divfadeout+1),a
		add	a,a
		ld	(countfadeout+1),a
		ret

;write A byte to OndraMELODIK
outsn:
		out (LS374), a
		ld a, 0111b ;strobe
		out (LS173), a
		ld a, 1111b
		out (LS173), a
		ret
		
; SN76489 write routines
sn76489_write_volume:
    ; Write volume command to SN76489
    ld a, (hl)
    xor 0fh ;AY volume to SN
    and 0fh
    add a,d
    call outsn  
    ret
    
sn76489_write_noise:
    ;3 noise freqs of SN76489
    cp 10
    jr c, .low
    cp 20
    jr nc, .high
    ld a, 1 ;middle (default)
    jr .selected
.low
	ld a, 2
	jr .selected
.high
	ld a, 0
	jr .selected
.selected
   
    add a,d
    call outsn
    ret

; Write frequency command for AY converted for SN76489
;channel in D
sn76489_write_frequency:
    ld a,(hl)
    ld c,a
    ld e,a
    rrca
    rrca
    rrca
    rrca
    and 0fh
    ld e,a
    ld a,c
    and 0fh
    add a,d
    call outsn
    inc hl
    ld a, (hl)
    rlca
    rlca
    rlca
    rlca
    and 0f0h
    add a,e
    res 7,a
    call outsn
    dec hl
    ret

;------------------------------------------------------------------------------
positions:		dw	NULL
ornaments:		dw	NULL
patterns:		dw	NULL
samples:		dw	NULL
speed:			db	0
tempo_cnt:		db	0
song_length:	db	0

chn1_pattptr:	dw	empty_pattern
chn2_pattptr:	dw	empty_pattern
chn3_pattptr:	dw	empty_pattern
empty_pattern:	db	#FF
mute_volume:	db  0

VARS
CHN1:		ds	10
CHN2:		ds	10
CHN3:		ds	10

current_pos:	db	0
@VARSLEN = $ - VARS

AYREGS:		ds	14

;Adopted to 4MHz quartz SN76489 lower freq range
;------------------------------------------------------------------------------
freqtable:

		dw	#03BA, #0383, #0352, #0321, #02F6, #02CA, #02A4, #027E
		dw	#0259, #0238, #0218, #03F8, #03BA, #0383, #0352, #0321
		dw	#02F6, #02CA, #02A4, #027E, #0259, #0238, #0218, #03F8
		dw	#03BA, #0383, #0352, #0321, #02F6, #02CA, #02A4, #027E
		dw	#0259, #0238, #0218, #01FA, #01DD, #01C3, #01A9, #0190
		dw	#017B, #0166, #0152, #013F, #012D, #011C, #010C, #00FD
		dw	#00EF, #00E2, #00D5, #00C9, #00BE, #00B3, #00A9, #009F
		dw	#0096, #008E, #0086, #007F, #0077, #0071, #006A, #0064
		dw	#005F, #0059, #0054, #0050, #004B, #0047, #0043, #003F
		dw	#003C, #0038, #0035, #0032, #002F, #002D, #002A, #0028
		dw	#0026, #0024, #0022, #0020, #001E, #001C, #001B, #0019
		dw	#0018, #0016, #0015, #0014, #0013, #0012, #0011, #0010

;------------------------------------------------------------------------------

	struct AR	; AY Registers structure
TonA		word	; 0
TonB		word	; 2
TonC		word	; 4
Noise		byte	; 6
Mixer		byte	; 7
AmplA		byte	; 8
AmplB		byte	; 9
AmplC		byte	; 10
	ends

	struct CHP	; channel parameters structure
EnvState	byte
PatStep		byte
SmpIndex	byte
Note		byte
PatStpCnt	byte
SmpPtr		word
OrnPtr		word
SmpCnt		byte
	ends


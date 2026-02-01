.data
nl: .asciiz "\n"

PI: .word 0
G: .word 0
Z: .word 0
str_0: .asciiz "small"
str_1: .asciiz "medium"
str_2: .asciiz "large"
str_3: .asciiz "FINAL_TEST"
str_4: .asciiz "break"
str_5: .asciiz "range_ok"
str_6: .asciiz "range_bad"
str_7: .asciiz "done"
flt_0: .float 3.0
flt_1: .float 2.0

.text
.globl main

function_int_sum2:
  addiu $sp, $sp, -32
  sw $ra, 28($sp)
  sw $fp, 24($sp)
  addiu $fp, $sp, 32

  lw $t0, 4($fp)
  sw $t0, -16($fp)
  lw $t0, 0($fp)
  sw $t0, -20($fp)
  lw $t0, -16($fp)
  lw $t1, -20($fp)
  addu $t2, $t0, $t1
  sw $t2, -24($fp)
  lw $t0, -24($fp)
  sw $t0, -12($fp)
  lw $t0, -12($fp)
  sw $t0, -28($fp)
  lw $a0, -28($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -12($fp)
  sw $t0, -32($fp)
  lw $v0, -32($fp)
  j function_int_sum2_end
function_int_sum2_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 32
  jr $ra

function_int_absDiff:
  addiu $sp, $sp, -64
  sw $ra, 60($sp)
  sw $fp, 56($sp)
  addiu $fp, $sp, 64

absDiff_decide_1:
absDiff_decide_1_cond_1:
  lw $t0, 4($fp)
  sw $t0, -16($fp)
  lw $t0, 0($fp)
  sw $t0, -20($fp)
  lw $t0, -16($fp)
  lw $t1, -20($fp)
  slt $t2, $t0, $t1
  xori $t2, $t2, 1
  sw $t2, -24($fp)
  lw $t0, -24($fp)
  bne $t0, $zero, absDiff_decide_1_bloque_1
  j absDiff_decide_1_else
absDiff_decide_1_bloque_1:
  lw $t0, 4($fp)
  sw $t0, -28($fp)
  lw $t0, 0($fp)
  sw $t0, -32($fp)
  lw $t0, -28($fp)
  lw $t1, -32($fp)
  subu $t2, $t0, $t1
  sw $t2, -36($fp)
  lw $t0, -36($fp)
  sw $t0, -12($fp)
  j absDiff_decide_1_end
absDiff_decide_1_else:
  lw $t0, 0($fp)
  sw $t0, -40($fp)
  lw $t0, 4($fp)
  sw $t0, -44($fp)
  lw $t0, -40($fp)
  lw $t1, -44($fp)
  subu $t2, $t0, $t1
  sw $t2, -48($fp)
  lw $t0, -48($fp)
  sw $t0, -12($fp)
  j absDiff_decide_1_end
absDiff_decide_1_end:
  lw $t0, -12($fp)
  sw $t0, -52($fp)
  lw $a0, -52($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -12($fp)
  sw $t0, -56($fp)
  lw $v0, -56($fp)
  j function_int_absDiff_end
function_int_absDiff_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 64
  jr $ra

function_int_classify:
  addiu $sp, $sp, -64
  sw $ra, 60($sp)
  sw $fp, 56($sp)
  addiu $fp, $sp, 64

classify_decide_1:
classify_decide_1_cond_1:
  lw $t0, 0($fp)
  sw $t0, -12($fp)
  li $t0, 5
  sw $t0, -16($fp)
  lw $t0, -12($fp)
  lw $t1, -16($fp)
  slt $t2, $t0, $t1
  sw $t2, -20($fp)
  lw $t0, -20($fp)
  bne $t0, $zero, classify_decide_1_bloque_1
  j classify_decide_1_cond_2
classify_decide_1_bloque_1:
  la $t0, str_0
  sw $t0, -24($fp)
  lw $a0, -24($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 0
  sw $t0, -28($fp)
  lw $v0, -28($fp)
  j function_int_classify_end
  j classify_decide_1_end
classify_decide_1_cond_2:
  lw $t0, 0($fp)
  sw $t0, -32($fp)
  li $t0, 20
  sw $t0, -36($fp)
  lw $t0, -32($fp)
  lw $t1, -36($fp)
  slt $t2, $t0, $t1
  sw $t2, -40($fp)
  lw $t0, -40($fp)
  bne $t0, $zero, classify_decide_1_bloque_2
  j classify_decide_1_else
classify_decide_1_bloque_2:
  la $t0, str_1
  sw $t0, -44($fp)
  lw $a0, -44($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 0
  sw $t0, -48($fp)
  lw $v0, -48($fp)
  j function_int_classify_end
  j classify_decide_1_end
classify_decide_1_else:
  la $t0, str_2
  sw $t0, -52($fp)
  lw $a0, -52($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 0
  sw $t0, -56($fp)
  lw $v0, -56($fp)
  j function_int_classify_end
  j classify_decide_1_end
classify_decide_1_end:
function_int_classify_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 64
  jr $ra

function_float_areaCircle:
  addiu $sp, $sp, -48
  sw $ra, 44($sp)
  sw $fp, 40($sp)
  addiu $fp, $sp, 48

  lw $t0, PI
  sw $t0, -16($fp)
  l.s $f0, 0($fp)
  s.s $f0, -20($fp)
  l.s $f0, -16($fp)
  l.s $f1, -20($fp)
  mul.s $f2, $f0, $f1
  s.s $f2, -24($fp)
  l.s $f0, -24($fp)
  s.s $f0, -12($fp)
  l.s $f0, -12($fp)
  s.s $f0, -28($fp)
  l.s $f0, 0($fp)
  s.s $f0, -32($fp)
  l.s $f0, -28($fp)
  l.s $f1, -32($fp)
  mul.s $f2, $f0, $f1
  s.s $f2, -36($fp)
  l.s $f0, -36($fp)
  s.s $f0, -12($fp)
  l.s $f0, -12($fp)
  s.s $f0, -40($fp)
  l.s $f12, -40($fp)
  li $v0, 2
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  l.s $f0, -12($fp)
  s.s $f0, -44($fp)
  l.s $f0, -44($fp)
  j function_float_areaCircle_end
function_float_areaCircle_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 48
  jr $ra

function_bool_inRange:
  addiu $sp, $sp, -64
  sw $ra, 60($sp)
  sw $fp, 56($sp)
  addiu $fp, $sp, 64

  lw $t0, 8($fp)
  sw $t0, -24($fp)
  lw $t0, 4($fp)
  sw $t0, -28($fp)
  lw $t0, -24($fp)
  lw $t1, -28($fp)
  slt $t2, $t0, $t1
  xori $t2, $t2, 1
  sw $t2, -32($fp)
  lw $t0, -32($fp)
  sw $t0, -12($fp)
  lw $t0, 8($fp)
  sw $t0, -36($fp)
  lw $t0, 0($fp)
  sw $t0, -40($fp)
  lw $t0, -36($fp)
  lw $t1, -40($fp)
  slt $t2, $t1, $t0
  xori $t2, $t2, 1
  sw $t2, -44($fp)
  lw $t0, -44($fp)
  sw $t0, -16($fp)
  lw $t0, -12($fp)
  sw $t0, -48($fp)
  lw $t0, -16($fp)
  sw $t0, -52($fp)
  lw $t0, -48($fp)
  lw $t1, -52($fp)
  sltu $t3, $zero, $t0
  sltu $t4, $zero, $t1
  and  $t2, $t3, $t4
  sw $t2, -56($fp)
  lw $t0, -56($fp)
  sw $t0, -20($fp)
  lw $t0, -20($fp)
  sw $t0, -60($fp)
  lw $a0, -60($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -20($fp)
  sw $t0, -64($fp)
  lw $v0, -64($fp)
  j function_bool_inRange_end
function_bool_inRange_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 64
  jr $ra

main:
  addiu $sp, $sp, -400
  sw $ra, 396($sp)
  sw $fp, 392($sp)
  addiu $fp, $sp, 400

  li $t0, 2
  sw $t0, -60($fp)
  lw $t0, -60($fp)
  sw $t0, G
  l.s $f0, flt_0
  s.s $f0, -64($fp)
  l.s $f0, -64($fp)
  s.s $f0, PI
  li $t0, 0
  sw $t0, -68($fp)
  lw $t0, -68($fp)
  sw $t0, Z
  la $t0, str_3
  sw $t0, -72($fp)
  lw $t0, -72($fp)
  sw $t0, -56($fp)
  lw $t0, -56($fp)
  sw $t0, -76($fp)
  lw $a0, -76($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 75
  sw $t0, -80($fp)
  lw $t0, -80($fp)
  sw $t0, -52($fp)
  lw $t0, -52($fp)
  sw $t0, -84($fp)
  lw $a0, -84($fp)
  li $v0, 11
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 0
  sw $t0, -88($fp)
  lw $t0, -88($fp)
  sw $t0, -20($fp)
  li $t0, 0
  sw $t0, -92($fp)
  lw $t0, -92($fp)
  sw $t0, -12($fp)
main_for_1:
main_for_1_ini:
  lw $t0, -12($fp)
  sw $t0, -96($fp)
  lw $t0, -96($fp)
  sw $t0, -12($fp)
main_for_1_cond:
  lw $t0, -12($fp)
  sw $t0, -100($fp)
  li $t0, 10
  sw $t0, -104($fp)
  lw $t0, -100($fp)
  lw $t1, -104($fp)
  slt $t2, $t0, $t1
  sw $t2, -108($fp)
  lw $t0, -108($fp)
  bne $t0, $zero, main_for_1_bloque
  j main_for_1_end
main_for_1_modif:
  lw $t0, -12($fp)
  sw $t0, -112($fp)
  lw $t0, -112($fp)
  li $t1, 1
  addu $t2, $t0, $t1
  sw $t2, -116($fp)
  lw $t0, -116($fp)
  sw $t0, -12($fp)
  j main_for_1_cond
main_for_1_bloque:
  lw $t0, -12($fp)
  sw $t0, -120($fp)
  lw $t0, -120($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  lw $t0, G
  sw $t0, -124($fp)
  lw $t0, -124($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  jal function_int_sum2
  addiu $sp, $sp, 8
  sw $v0, -128($fp)
  lw $t0, -128($fp)
  sw $t0, -24($fp)
  lw $t0, -24($fp)
  sw $t0, -132($fp)
  lw $t0, -132($fp)
  sw $t0, -16($fp)
  lw $t0, -16($fp)
  sw $t0, -136($fp)
  lw $a0, -136($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -16($fp)
  sw $t0, -140($fp)
  lw $t0, -140($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  jal function_int_classify
  addiu $sp, $sp, 4
  sw $v0, -144($fp)
  lw $t0, -144($fp)
  sw $t0, -24($fp)
  lw $t0, -20($fp)
  sw $t0, -148($fp)
  lw $t0, -16($fp)
  sw $t0, -152($fp)
  lw $t0, -148($fp)
  lw $t1, -152($fp)
  addu $t2, $t0, $t1
  sw $t2, -156($fp)
  lw $t0, -156($fp)
  sw $t0, -20($fp)
main_decide_1:
main_decide_1_cond_1:
  lw $t0, -12($fp)
  sw $t0, -160($fp)
  li $t0, 4
  sw $t0, -164($fp)
  lw $t0, -160($fp)
  lw $t1, -164($fp)
  xor $t2, $t0, $t1
  sltiu $t2, $t2, 1
  sw $t2, -168($fp)
  lw $t0, -168($fp)
  bne $t0, $zero, main_decide_1_bloque_1
  j main_decide_1_end
main_decide_1_bloque_1:
  la $t0, str_4
  sw $t0, -172($fp)
  lw $a0, -172($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  j main_for_1_end
  j main_decide_1_end
main_decide_1_end:
  j main_for_1_modif
main_for_1_end:
  lw $t0, -20($fp)
  sw $t0, -176($fp)
  lw $a0, -176($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -20($fp)
  sw $t0, -180($fp)
  li $t0, 10
  sw $t0, -184($fp)
  lw $t0, -180($fp)
  lw $t1, -184($fp)
  slt $t2, $t0, $t1
  xori $t2, $t2, 1
  sw $t2, -188($fp)
  lw $t0, -188($fp)
  sw $t0, -28($fp)
  lw $t0, -20($fp)
  sw $t0, -192($fp)
  li $t0, 30
  sw $t0, -196($fp)
  lw $t0, -192($fp)
  lw $t1, -196($fp)
  slt $t2, $t1, $t0
  xori $t2, $t2, 1
  sw $t2, -200($fp)
  lw $t0, -200($fp)
  sw $t0, -32($fp)
  lw $t0, -28($fp)
  sw $t0, -204($fp)
  lw $t0, -32($fp)
  sw $t0, -208($fp)
  lw $t0, -204($fp)
  lw $t1, -208($fp)
  sltu $t3, $zero, $t0
  sltu $t4, $zero, $t1
  and  $t2, $t3, $t4
  sw $t2, -212($fp)
  lw $t0, -212($fp)
  sw $t0, -36($fp)
  lw $t0, -36($fp)
  sw $t0, -216($fp)
  lw $t0, -216($fp)
  sltu $t1, $zero, $t0
  xori $t2, $t1, 1
  sw $t2, -220($fp)
  lw $t0, -220($fp)
  sw $t0, -40($fp)
  lw $t0, -28($fp)
  sw $t0, -224($fp)
  lw $a0, -224($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -32($fp)
  sw $t0, -228($fp)
  lw $a0, -228($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -36($fp)
  sw $t0, -232($fp)
  lw $a0, -232($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -40($fp)
  sw $t0, -236($fp)
  lw $a0, -236($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
main_decide_2:
main_decide_2_cond_1:
  lw $t0, -36($fp)
  sw $t0, -240($fp)
  li $t0, 1
  sw $t0, -244($fp)
  lw $t0, -240($fp)
  lw $t1, -244($fp)
  xor $t2, $t0, $t1
  sltiu $t2, $t2, 1
  sw $t2, -248($fp)
  lw $t0, -248($fp)
  bne $t0, $zero, main_decide_2_bloque_1
  j main_decide_2_else
main_decide_2_bloque_1:
  la $t0, str_5
  sw $t0, -252($fp)
  lw $a0, -252($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  j main_decide_2_end
main_decide_2_else:
  la $t0, str_6
  sw $t0, -256($fp)
  lw $a0, -256($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  j main_decide_2_end
main_decide_2_end:
  lw $t0, -20($fp)
  sw $t0, -260($fp)
  lw $t0, -260($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  li $t0, 17
  sw $t0, -264($fp)
  lw $t0, -264($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  jal function_int_absDiff
  addiu $sp, $sp, 8
  sw $v0, -268($fp)
  lw $t0, -268($fp)
  sw $t0, -24($fp)
  lw $t0, -24($fp)
  sw $t0, -272($fp)
  lw $a0, -272($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -24($fp)
  sw $t0, -276($fp)
  lw $t0, -276($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  li $t0, 0
  sw $t0, -280($fp)
  lw $t0, -280($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  li $t0, 10
  sw $t0, -284($fp)
  lw $t0, -284($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  jal function_bool_inRange
  addiu $sp, $sp, 12
  sw $v0, -288($fp)
  lw $t0, -288($fp)
  sw $t0, -28($fp)
  lw $t0, -24($fp)
  sw $t0, -292($fp)
  li $t0, 3
  sw $t0, -296($fp)
  lw $t0, -292($fp)
  lw $t1, -296($fp)
  xor $t2, $t0, $t1
  sltiu $t2, $t2, 1
  sw $t2, -300($fp)
  lw $t0, -300($fp)
  sw $t0, -32($fp)
  lw $t0, -28($fp)
  sw $t0, -304($fp)
  lw $t0, -32($fp)
  sw $t0, -308($fp)
  lw $t0, -304($fp)
  lw $t1, -308($fp)
  sltu $t3, $zero, $t0
  sltu $t4, $zero, $t1
  or   $t2, $t3, $t4
  sw $t2, -312($fp)
  lw $t0, -312($fp)
  sw $t0, -36($fp)
  lw $t0, -36($fp)
  sw $t0, -316($fp)
  lw $a0, -316($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  l.s $f0, flt_1
  s.s $f0, -320($fp)
  l.s $f0, -320($fp)
  s.s $f0, -44($fp)
  l.s $f0, -44($fp)
  s.s $f0, -324($fp)
  l.s $f0, -324($fp)
  addiu $sp, $sp, -4
  s.s $f0, 0($sp)
  jal function_float_areaCircle
  addiu $sp, $sp, 4
  sw $v0, -328($fp)
  lw $t0, -328($fp)
  sw $t0, -48($fp)
main_loop_1:
main_loop_1_body:
  lw $t0, Z
  sw $t0, -332($fp)
  li $t0, 1
  sw $t0, -336($fp)
  lw $t0, -332($fp)
  lw $t1, -336($fp)
  addu $t2, $t0, $t1
  sw $t2, -340($fp)
  lw $t0, -340($fp)
  sw $t0, Z
  lw $t0, Z
  sw $t0, -344($fp)
  lw $a0, -344($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, Z
  sw $t0, -348($fp)
  li $t0, 5
  sw $t0, -352($fp)
  lw $t0, -348($fp)
  lw $t1, -352($fp)
  slt $t2, $t0, $t1
  xori $t2, $t2, 1
  sw $t2, -356($fp)
  lw $t0, -356($fp)
  bne $t0, $zero, main_loop_1_end
  j main_loop_1_body
main_loop_1_end:
main_loop_2:
main_loop_2_body:
  lw $t0, -20($fp)
  sw $t0, -360($fp)
  li $t0, 3
  sw $t0, -364($fp)
  lw $t0, -360($fp)
  lw $t1, -364($fp)
  addu $t2, $t0, $t1
  sw $t2, -368($fp)
  lw $t0, -368($fp)
  sw $t0, -20($fp)
  lw $t0, -20($fp)
  sw $t0, -372($fp)
  lw $a0, -372($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  lw $t0, -20($fp)
  sw $t0, -376($fp)
  li $t0, 29
  sw $t0, -380($fp)
  lw $t0, -376($fp)
  lw $t1, -380($fp)
  slt $t2, $t0, $t1
  xori $t2, $t2, 1
  sw $t2, -384($fp)
  lw $t0, -384($fp)
  bne $t0, $zero, main_loop_2_end
  j main_loop_2_body
main_loop_2_end:
  la $t0, str_7
  sw $t0, -388($fp)
  lw $a0, -388($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 0
  sw $t0, -392($fp)
  lw $v0, -392($fp)
  j main_end
main_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 400
  jr $ra


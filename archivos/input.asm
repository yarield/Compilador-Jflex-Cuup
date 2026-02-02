.data
nl: .asciiz "\n"

i: .word 0

.text
.globl main

main:
  addiu $sp, $sp, -48
  sw $ra, 44($sp)
  sw $fp, 40($sp)
  addiu $fp, $sp, 48

  lw $t0, -12($fp)
  sw $t0, -16($fp)
  lw $a0, -16($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
main_for_1:
main_for_1_ini:
  li $t0, 0
  sw $t0, -20($fp)
  lw $t0, -20($fp)
  sw $t0, i
main_for_1_cond:
  lw $t0, i
  sw $t0, -24($fp)
  li $t0, 5
  sw $t0, -28($fp)
  lw $t0, -24($fp)
  lw $t1, -28($fp)
  slt $t2, $t0, $t1
  sw $t2, -32($fp)
  lw $t0, -32($fp)
  bne $t0, $zero, main_for_1_bloque
  j main_for_1_end
main_for_1_modif:
  lw $t0, i
  sw $t0, -36($fp)
  lw $t0, -36($fp)
  li $t1, 1
  addu $t2, $t0, $t1
  sw $t2, -40($fp)
  lw $t0, -40($fp)
  sw $t0, i
  j main_for_1_cond
main_for_1_bloque:
  lw $t0, i
  sw $t0, -44($fp)
  lw $a0, -44($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  j main_for_1_modif
main_for_1_end:
main_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 48
  jr $ra


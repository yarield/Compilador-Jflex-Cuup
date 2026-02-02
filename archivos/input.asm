.data
nl: .asciiz "\n"

globalpipipi: .word 0
str_0: .asciiz "mayor a 10"
str_1: .asciiz "mayor a 5"
str_2: .asciiz "menor a 5"

.text
.globl main

function_int_func:
  addiu $sp, $sp, -32
  sw $ra, 28($sp)
  sw $fp, 24($sp)
  addiu $fp, $sp, 32

  lw $t0, 0($fp)
  sw $t0, -16($fp)
  li $t0, 5
  sw $t0, -20($fp)
  lw $t0, -16($fp)
  lw $t1, -20($fp)
  addu $t2, $t0, $t1
  sw $t2, -24($fp)
  lw $t0, -24($fp)
  sw $t0, -12($fp)
  lw $t0, -12($fp)
  sw $t0, -28($fp)
  lw $v0, -28($fp)
  j function_int_func_end
function_int_func_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 32
  jr $ra

main:
  addiu $sp, $sp, -80
  sw $ra, 76($sp)
  sw $fp, 72($sp)
  addiu $fp, $sp, 80

  li $t0, 4
  sw $t0, -16($fp)
  lw $t0, -16($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  jal function_int_func
  addiu $sp, $sp, 4
  sw $v0, -20($fp)
  lw $a0, -20($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 6
  sw $t0, -24($fp)
  lw $t0, -24($fp)
  addiu $sp, $sp, -4
  sw $t0, 0($sp)
  jal function_int_func
  addiu $sp, $sp, 4
  sw $v0, -28($fp)
  lw $t0, -28($fp)
  sw $t0, -12($fp)
  lw $t0, -12($fp)
  sw $t0, -32($fp)
  lw $a0, -32($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
main_decide_1:
main_decide_1_cond_1:
  lw $t0, -12($fp)
  sw $t0, -36($fp)
  li $t0, 10
  sw $t0, -40($fp)
  lw $t0, -36($fp)
  lw $t1, -40($fp)
  slt $t2, $t1, $t0
  sw $t2, -44($fp)
  lw $t0, -44($fp)
  bne $t0, $zero, main_decide_1_bloque_1
  j main_decide_1_cond_2
main_decide_1_bloque_1:
  la $t0, str_0
  sw $t0, -48($fp)
  lw $a0, -48($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  j main_decide_1_end
main_decide_1_cond_2:
  lw $t0, -12($fp)
  sw $t0, -52($fp)
  li $t0, 5
  sw $t0, -56($fp)
  lw $t0, -52($fp)
  lw $t1, -56($fp)
  slt $t2, $t1, $t0
  sw $t2, -60($fp)
  lw $t0, -60($fp)
  bne $t0, $zero, main_decide_1_bloque_2
  j main_decide_1_else
main_decide_1_bloque_2:
  la $t0, str_1
  sw $t0, -64($fp)
  lw $a0, -64($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  j main_decide_1_end
main_decide_1_else:
  la $t0, str_2
  sw $t0, -68($fp)
  lw $a0, -68($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  j main_decide_1_end
main_decide_1_end:
  lw $t0, globalpipipi
  sw $t0, -72($fp)
  lw $a0, -72($fp)
  li $v0, 1
  syscall
  la $a0, nl
  li $v0, 4
  syscall
main_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 80
  jr $ra


.data
nl: .asciiz "\n"

str_0: .asciiz "Hola mundo"

.text
.globl main

main:
  addiu $sp, $sp, -32
  sw $ra, 28($sp)
  sw $fp, 24($sp)
  addiu $fp, $sp, 32

  la $t0, str_0
  sw $t0, -16($fp)
  lw $t0, -16($fp)
  sw $t0, -12($fp)
  lw $t0, -12($fp)
  sw $t0, -20($fp)
  lw $a0, -20($fp)
  li $v0, 4
  syscall
  la $a0, nl
  li $v0, 4
  syscall
  li $t0, 0
  sw $t0, -24($fp)
  lw $v0, -24($fp)
  j main_end
main_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 32
  jr $ra


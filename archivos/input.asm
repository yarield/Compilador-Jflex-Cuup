.data
nl: .asciiz "\n"


.text
.globl main

main:
  addiu $sp, $sp, -16
  sw $ra, 12($sp)
  sw $fp, 8($sp)
  addiu $fp, $sp, 16

main_end:
  lw $ra, -4($fp)
  lw $fp, -8($fp)
  addiu $sp, $sp, 16
  jr $ra


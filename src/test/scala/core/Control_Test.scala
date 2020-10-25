/******************************************************************
* Filename:      Control_Test.scala
* Date:          04-04-2020
* Author:        M Tahir
*
* Description:   This is a simple test based on Chisel3 iotesters  
*                using FIRRTL compiler.
*
* Issues:        The iotesters is failing when the instruction 
*                involes a negative immediate or if the bit 31 is 
*                1. The following error is reported by the compiler.
*                "java.lang.IllegalArgumentException: requirement 
*                failed: UInt literal -3945 is negative" 
*                 
******************************************************************/

package riscv_uet

import chisel3.UInt
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import chisel3.util
import org.scalatest.{FlatSpec, Matchers}

import CONSTANTS._
import ALUOP._


class TestControl(c: Control) extends PeekPokeTester(c) {
  
  // auipc ra, 0x7ffff  
  var inst = 0x7ffff097.U
  poke(c.io.inst, inst)
  step(1)
  expect(c.io.alu_op, ALU_ADD)
  expect(c.io.imm_sel, IMM_U) 
  expect(c.io.pc_sel, PC_4) 

  // jal s0, 353190
  inst = 0x3a65646f.U
  poke(c.io.inst, inst)
  step(1)
  expect(c.io.alu_op, ALU_ADD)
  expect(c.io.imm_sel, IMM_J) 
  expect(c.io.pc_sel, PC_ALU) 

// beq a1, a0, 40
  inst = 0x02a58463.U
  poke(c.io.inst, inst)
  step(1)
  expect(c.io.br_type, BR_EQ)
  expect(c.io.imm_sel, IMM_B) 
  expect(c.io.pc_sel, PC_4) 
  step(1)

}


object Control_Main extends App {
  /* Driver.execute(args, () => new Control) {
    (c) => new TestControl(c)
  } */
  
  iotesters.Driver.execute(Array("--generate-vcd-output", "on", "--backend-name", "firrtl"), () => new Control) {
    c => new TestControl(c)
  } 
}

/******************************************************************
* Filename:      ImmGen_Test.scala
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


class TestImmGen(c: ImmGen) extends PeekPokeTester(c) {
  
  // auipc ra, 0x7ffff  
  var inst = 0x7ffff097.U
  var imm_sel = IMM_U
  poke(c.io.inst, inst)
  poke(c.io.sel, imm_sel)
 // peek(c.io.out)
  step(1)
  expect(c.io.out, 0x7ffff000.U)

  // jal s0, 353190
  inst = 0x3a65646f.U
  imm_sel = IMM_J
  poke(c.io.inst, inst)
  poke(c.io.sel, imm_sel)
//  peek(c.io.out)
  step(1)
  expect(c.io.out, 353190.U)
  step(1)

}


object ImmGen_Main extends App {
  /* Driver.execute(args, () => new ImmGen) {
    (c) => new TestImmGen(c)
  } */
  
  iotesters.Driver.execute(Array("--generate-vcd-output", "on", "--backend-name", "firrtl"), () => new ImmGen) {
    c => new TestImmGen(c)
  } 
}

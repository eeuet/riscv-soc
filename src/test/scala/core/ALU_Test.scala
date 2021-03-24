/******************************************************************
* Filename:      ALU_Test.scala
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

import ALUOP._

class TestALU(c: ALU) extends PeekPokeTester(c) {
  
  // ALU operands initialization
  var src_a = 100
  var src_b = 20
  
  // function definitions
  def unsignedCompare(x : Long, y : Long) = (x < y) ^ (x < 0) ^ (y < 0)
  
  def testAlu(aluop: UInt) = {
    
    val result = aluop match {
      case ALU_ADD  => src_a + src_b
      case ALU_SUB  => src_a - src_b
      case ALU_AND  => src_a & src_b
      case ALU_OR   => src_a | src_b
      case ALU_XOR  => src_a ^ src_b
      case ALU_SLL  => src_a  << (src_b & 0x1F)
      case ALU_SRL  => src_a >>> (src_b & 0x1F)
      case ALU_SRA  => src_a >> (src_b & 0x1F)
      case ALU_SLT  => (src_a < src_b).toInt
      case ALU_SLTU => unsignedCompare(src_a , src_b).toInt
      case _        => 0
    } 
    
    poke(c.io.in_A, src_a)
    poke(c.io.in_B, src_b)
    poke(c.io.alu_Op, aluop)
   // val result = peek(c.io.out)
   // peek(c.io.con_Flag)
    step(1)
    expect(c.io.out, BigInt(result.toHexString, 16))
  }
   
  testAlu(ALU_ADD)
  testAlu(ALU_SUB)
  testAlu(ALU_AND)
  testAlu(ALU_OR)
  step(1)

}

/*
class ALU_Spce extends FlatSpec with Matchers {
  iotesters.Driver.execute((Array("--backend-name", "verilator")), () => new ALU) {
    c => new ALU_Test(c)
  }

  iotesters.Driver.execute((Array("--is-verbose")), () => new ALU) {
    c => new ALU_Test(c)
  }
} */

object ALU_Main extends App {
  /* Driver.execute(args, () => new ALU) {
    (c) => new TestALU(c)
  } */
  
  iotesters.Driver.execute(Array("--generate-vcd-output", "on", "--backend-name", "firrtl"), () => new ALU) {
    c => new TestALU(c)
  } 
}

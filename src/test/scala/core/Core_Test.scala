/******************************************************************
* Filename:      Core_Test.scala
* Date:          04-04-2020
* Author:        M Tahir
*
* Description:   This is a simple test based on Chisel3 iotesters  
*                using Treadle compiler.
*
* Issues:        
*                 
******************************************************************/

package riscv_uet

import chisel3._
import chisel3.util
import chisel3.UInt
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import scala.io.Source
import org.scalatest.{FlatSpec, Matchers}
import java.io.{File, PrintWriter}

import CONSTANTS._
import ALUOP._
// import utils.ArgParser


class TestCore(c: ProcessorTile) // traceFile: String, genTrace: Boolean
      extends PeekPokeTester(c) {
  //val endFlag = BigInt("deadc0de", 16)

      step(1)
      step(1)
      step(1)
      step(1)

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")
      expect(c.io.debug.regWaddr, 0x06)

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")
      expect(c.io.debug.regWaddr, 0x0A) 

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")
      expect(c.io.debug.regWaddr, 0x0B) 

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")
      expect(c.io.debug.regWaddr, 0x0C) 

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")
      expect(c.io.debug.regWaddr, 0x05) 

      for (i <- 0 until 16) {
        step(1)
      }

      var uart_Int = true.B
   //   poke(c.io.irq.uartIrq, uart_Int)

      for (i <- 0 until 10) {
            step(1)
      }
   //   uart_Int = false.B
   //   poke(c.io.irq.uartIrq, uart_Int)

      for (i <- 0 until 8) {
            step(1)
      }

      uart_Int = true.B
      poke(c.io.irq.uartIrq, uart_Int)
      step(1)
      step(1)
      uart_Int = false.B
      poke(c.io.irq.uartIrq, uart_Int)

      for (i <- 0 until 200) {
            step(1)
      }

}


object Core_Main extends App {
  var initFile = "src/test/resources/main.txt"
  
  iotesters.Driver.execute(Array("--generate-vcd-output", "on", "--backend-name", "treadle"), () => new ProcessorTile(initFile)) {
    c => new TestCore(c)
  } 

}


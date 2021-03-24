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

      var uart_TxRx = (peek(c.io.uartTx) == BigInt(1))

      for (i <- 0 until 4000) {
            uart_TxRx = (peek(c.io.uartTx) == BigInt(1) )
            if (uart_TxRx)
                  poke(c.io.uartRx, true.B)
            else
                  poke(c.io.uartRx, false.B)
            step(1)
      }



}


object Core_Main extends App {
  var initFile = "src/test/resources/main.txt"
  
  iotesters.Driver.execute(Array("--generate-vcd-output", "on", "--backend-name", "treadle"), () => new ProcessorTile(initFile)) {
    c => new TestCore(c)
  } 

}


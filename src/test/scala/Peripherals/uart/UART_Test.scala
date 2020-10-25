/******************************************************************
* Filename:      UART_Test.scala
* Date:          16-04-2020
* Author:        M Tahir
*
* Description:   This is a simple test based on Chisel3 iotesters  
*                using FIRRTL compiler.
*
******************************************************************/

package uart

import chisel3.UInt
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import chisel3.util
import org.scalatest.{FlatSpec, Matchers}

class TestUart(c: UART) extends PeekPokeTester(c) {

 // var data = 0x4A.U
 //  val enable = 1.U(1.W)
 // poke(c.io.in, data)
  poke(c.io.en, 1.U)
 // peek(c.io.out)
  step(1)

  for (i <- 0 until 255) {
    step(1)
  }
 // enable := 0.U
  poke(c.io.en, 0.U)
  for (i <- 0 until 20) {
    step(1)
  }
}


object Uart_Main extends App {
  /* Driver.execute(args, () => new UARTTx) {
    (c) => new TestUart(c)
  } */
  val ut = UARTParams()
  
  iotesters.Driver.execute(Array("--generate-vcd-output", "on", "--backend-name", "firrtl"), () => new UART(ut)){
    c => new TestUart(c)
  } 
}

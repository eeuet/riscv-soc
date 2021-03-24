/*********************************************************************
* Filename :    ProcessorTile.scala
* Date     :    28-03-2020
* 
* Description:  Based on riscv mini. Modified cache memory interface
*               to simple Inst/Data memory interfaces
*
* 13-04-2020    The DebugIO interface is being used for testing.
*     
*          
*               An object code file 'fib.txt' is used to load program
*               to code memory for execution during testing. 
*********************************************************************/


package riscv_uet

import chisel3._
import chisel3.util._
import wishbone.WB_InterConnect
import memories.DataMem
import memories.{DataMem, InstMem}


class TileIO extends Bundle {
  val debug  = new DebugIO
 // val irq  = new IrqIO
  val uartTx = Output(Bool())
  val uartRx = Input(Bool())
}

trait TileBase extends core.BaseModule {
  def io: TileIO
  def clock: Clock
  def reset: core.Reset
}

class ProcessorTile(initFile: String) extends Module with TileBase {
  val io     = IO(new TileIO)
  val core   = Module(new Core)
 // val imem = Module(new InstMem(initFile))
 // val dmem = Module(new DataMem)
  val wb_InterConnect = Module(new WB_InterConnect(initFile))
 
  io.debug  <> core.io.debug
 // io.irq  <> core.io.irq
  core.io.imemIO <> wb_InterConnect.io.imemIO
  core.io.dmemIO <> wb_InterConnect.io.dmemIO
//  wb_InterConnect.io.wbm <> dmem.io.wbs

  // Connection for UART interface
  io.uartTx := wb_InterConnect.io.uartTx
  wb_InterConnect.io.uartRx := io.uartRx
  core.io.irq.uartIrq := wb_InterConnect.io.uartInt
}

object ProcessorTile_generate extends App {
var initFile = "src/test/resources/main.txt"

  chisel3.Driver.execute(args, () => new ProcessorTile(initFile))
} 


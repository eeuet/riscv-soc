/*********************************************************************
* Filename :    Core.scala
* Date     :    28-03-2020
* 
* Description:  Based on riscv mini. Modified cache memory interface
*               to simple Inst/Data memory interfaces
*
* 13-04-2020    The HostIO interface is temporarily disabled. 
*********************************************************************/

package riscv_uet

import chisel3._
import chisel3.util.Valid


class DMemIO extends Bundle with Config {
  val addr        = Input(UInt(WLEN.W))
  val wdata       = Input(UInt(WLEN.W))
  val rd_en       = Input(UInt(MEM_READ_SIG_LEN.W))
  val wr_en       = Input(UInt(MEM_WRITE_SIG_LEN.W))
  val st_type     = Input(UInt(DATA_SIZE_SIG_LEN.W))
  val ld_type     = Input(UInt(LOAD_TYPE_SIG_LEN.W))
  val rdata       = Output(UInt(WLEN.W))
  val valid       = Output(Bool())
}

class IMemIO extends Bundle with Config {
  val addr = Input(UInt(WLEN.W))
  val inst = Output(UInt(WLEN.W))
  val inst_valid       = Output(Bool())
  // Wishbone slave interface for reading data from instruction memory
 // val wbs  = Flipped(new WishboneIO)
}

class DebugIO extends Bundle with Config {
  val inst      = Output(UInt(XLEN.W))
  val regWaddr  = Output(UInt(5.W))
  val regWdata  = Output(UInt(XLEN.W))
  val pc        = Output(UInt(XLEN.W))
}

class IrqIO extends Bundle {
  val uartIrq  = Input(Bool())
 // val timerIrq  = Input(Bool())
}

class CoreIO extends Bundle {
  val debug = new DebugIO
  val irq   = new IrqIO
  val imemIO  = Flipped((new IMemIO))
  val dmemIO  = Flipped((new DMemIO))
}

class Core extends Module {
  val io = IO(new CoreIO)
  val dpath = Module(new Datapath) 
  val ctrl  = Module(new Control)

  io.debug <> dpath.io.debug
  io.irq <> dpath.io.irq
  dpath.io.imem <> io.imemIO
  dpath.io.dmem <> io.dmemIO
  dpath.io.ctrl <> ctrl.io

}

object Core_generate extends App {
  chisel3.Driver.execute(args, () => new Core)
} 


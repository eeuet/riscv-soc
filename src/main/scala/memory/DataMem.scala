/*********************************************************************
* Filename :    DataMem.scala
* Date     :    28-03-2020
* 
* Description:  Test data memory, 1024 Bytes, 32 Words, implemented 
*               using asynchronous read memory 'Mem'. The memory
 *              size can be changed in Configurations.scala.
*
* 13-04-2020    Supports variable length as well as unaligned
*               load and store
*********************************************************************/


package memories

import chisel3._
import chisel3.util._
import wishbone.WishboneSlaveIO
import riscv_uet.Control._
import chisel3.util.experimental.loadMemoryFromFile
import riscv_uet.CONSTANTS._
import riscv_uet.Config

class DataMemIO extends Bundle{
  val wbs  = new WishboneSlaveIO
}

class DataMem extends Module with Config {
 // val io = IO(new DataMemIO)
  val io = IO(new DataMemIO)

  // data memory is asynchronous read and synchronous write
  val dmem = Mem(DATA_MEM_LEN, UInt(BLEN.W))
 // loadMemoryFromFile(dmem, "resources/datamem.txt")

  val addr        = io.wbs.addr_i(11, 0)
  val wdata       = io.wbs.data_i
  val rd_en       = !io.wbs.we_i && io.wbs.stb_i
  val wr_en       = io.wbs.we_i && io.wbs.stb_i
  val st_type     = io.wbs.tgd_sttype_i

  val ack         = Reg(Bool())
  val rd_mux_sel  = Reg(Bool())
  io.wbs.ack_o   := ack
  ack            := io.wbs.stb_i
  rd_mux_sel     := rd_en

  // 'rdata' is used to synchronize the asynchronous reads
  val rdata = Reg(UInt(XLEN.W))
   rdata := Cat(dmem(addr + 3.U), dmem(addr + 2.U), dmem(addr + 1.U), dmem(addr))
  
    when (wr_en.toBool()) {
      when (st_type === 1.U) {
        dmem (addr)       := wdata(7,0)
        dmem (addr + 1.U) := wdata(15,8)
        dmem (addr + 2.U) := wdata(23,16)
        dmem (addr + 3.U) := wdata(31,24)
      }.elsewhen (st_type === 2.U) {
        dmem (addr)       := wdata(7,0)
        dmem (addr + 1.U) := wdata(15,8)
      }.elsewhen (st_type === 3.U) {
        dmem (addr)       := wdata(7,0)
      }
    }

  io.wbs.data_o  := Mux(rd_mux_sel, rdata, 0.U)
 //  io.wbs.data_i := rdata
}
   

/*
object DataMem extends App {
  chisel3.Driver.execute(args, () => new DataMem)
} */

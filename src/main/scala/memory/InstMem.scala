/*********************************************************************
* Filename :    InstMem.scala
* Date     :    28-03-2020
* 
* Description:  Test data memory, 1024 Bytes, 32 Words, implemented 
*               using 'mem'. The memory size can be changed in 
*               Configurations.scala.
*
* 13-04-2020    Supports word (32-bit) sized accesses and
 *              word/halfword/byte sized reads, in case of constant
 *              data, are possible.
*
*********************************************************************/


package memories

import chisel3._
import chisel3.util.{Cat, is, switch}
import riscv_uet.{CONSTANTS, Config, IMemIO}
import wishbone.{WB_Master, WishboneSlaveIO}
import chisel3.util.experimental.loadMemoryFromFile

import scala.io.Source
import java.io.{File, PrintWriter}


class InstMemIO extends Bundle with Config {
  val imemIO = (new IMemIO)
  // Wishbone slave interface for reading data from instruction memory
  val wbs  = new WishboneSlaveIO
}

class InstMem(initFile: String) extends Module with Config {
  val io = IO(new InstMemIO)

  // instruction memory with word aligned reads
  val imem = Mem(INST_MEM_LEN, UInt(WLEN.W))
  // loadMemoryFromFile(imem , "src/test/resources/text3.txt")
  loadMemoryFromFile(imem , initFile)

  // Dual bus interface for Instruction Memory. One interface is instruction-memory-IO (IMemIO) while other is Wishbone
  val ack            = Reg(Bool())                 // define ack signal
  val wb_select      = Reg(UInt(WB_SEL_SIZE.W))
  val wb_addr        = io.wbs.addr_i
  val wb_wdata       = io.wbs.data_i
  val wb_rd_en       = !io.wbs.we_i && io.wbs.stb_i
  val wb_wr_en       = io.wbs.we_i && io.wbs.stb_i
  val wb_st_type     = io.wbs.tgd_sttype_i

  // address decoding for reading constants from imem
  val imem_data_read = !(wb_addr(WB_IMEM_ADDR_HIGH, WB_IMEM_ADDR_LOW).orR) && wb_rd_en
  io.imemIO.inst_valid  := !imem_data_read

  // address Mux for selecting the source of transaction
  val imem_addr = Mux(imem_data_read, wb_addr, io.imemIO.addr)
  //read data/instruction from memory (val imem_addr = addr >> 2)
  val imem_read = imem(imem_addr / 4.U)

  val rd_const = Reg(UInt(XLEN.W))
  val rd_inst = Reg(UInt(XLEN.W))

  // DeMux to select between reading constants vs instructions
  when(imem_data_read) {
    rd_const := imem_read
    ack   := io.wbs.stb_i
  } otherwise {
    rd_inst := imem_read
  }

  // Use sel_o to determine which byte/half-word is addressed
   val rconst_data = WireInit(0.U(XLEN.W))
   wb_select      := io.wbs.sel_i

  when(wb_select === WB_Master.BYTE3_SEL) {
      rconst_data := rd_const(31, 24).asUInt
    }.elsewhen(wb_select === WB_Master.BYTE2_SEL){
      rconst_data := rd_const(23, 16).asUInt
    }.elsewhen(wb_select === WB_Master.BYTE1_SEL){
      rconst_data := rd_const(15, 8).asUInt
    }.elsewhen(wb_select === WB_Master.BYTE0_SEL){
      rconst_data := rd_const(7, 0).asUInt
    }.elsewhen(wb_select === WB_Master.HWORD_HIGH_SEL){
      rconst_data := rd_const(31, 16).asUInt
    }.elsewhen(wb_select === WB_Master.HWORD_LOW_SEL){
      rconst_data := rd_const(15, 0).asUInt
    }otherwise {
      rconst_data := rd_const(31, 0).asUInt
    }


  // Wishbone bus read constants from Instruction memory
  io.wbs.ack_o   := ack
  io.wbs.data_o  := rconst_data

  // Dedicated Inst bus read instructions
  io.imemIO.inst := rd_inst
}

/*
object InstMem extends App {
  chisel3.Driver.execute(args, () => new InstMem)
}
*/
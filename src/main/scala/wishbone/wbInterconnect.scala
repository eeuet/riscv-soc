/******************************************************************************
 * Filename :    wbInterconnect.scala
 * Date     :    20-05-2020
 *
 * Description:  Wishbone shared bus interface standard (ver. B4) based
 *               implementation.
 *
 *
 * 21-05-2020    Current implementation supports single master and only
 *               pipelined Read/Write operations.
 *******************************************************************************/

package wishbone

import chisel3._
import chisel3.util._
import chisel3.testers._
import chisel3.core.SeqUtils
import riscv_uet.{CONSTANTS, Config, DMemIO, IMemIO}
import memories.{DataMem, InstMem}
import uart.{UART, UartIO}

object WB_Master {
  val BYTE_0                = 0.U
  val BYTE_1                = 1.U
  val BYTE_2                = 2.U
  val BYTE_3                = 3.U

  val WORD_SEL              = 0xF.U(4.W)
  val HWORD_LOW_SEL         = 0x3.U(4.W)
  val HWORD_HIGH_SEL        = 0xC.U(4.W)
  val BYTE0_SEL             = 0x1.U(4.W)
  val BYTE1_SEL             = 0x2.U(4.W)
  val BYTE2_SEL             = 0x4.U(4.W)
  val BYTE3_SEL             = 0x8.U(4.W)
}

object WB_InterConnect {
  // Address mapping for Wishbone slaves
  val IMEM = 0x0.U(4.W)
  val DMEM = 0x1.U(4.W)
  val UART = 0x2.U(4.W)
}

class WB_MasterIO extends Bundle {
  val dmemIO  = new DMemIO
  val wbm  = (new WishboneMasterIO)
  //val wbmOut = new WishboneIO
}

class WB_Master extends Module with Config{
  val io = IO(new WB_MasterIO)

  // Wishbone Selection Output (sel_o) signal generation
  val d_align = io.dmemIO.addr(1, 0)
  val d_type = io.dmemIO.ld_type
  val sel_vec = WireInit(0.U(WB_SEL_SIZE.W))

  // generate the sel_o signal
  when(d_type === CONSTANTS.LD_LW) {
      sel_vec := WB_Master.WORD_SEL
    }
    .elsewhen(d_type === CONSTANTS.LD_LH || d_type === CONSTANTS.LD_LHU) {
      sel_vec := Mux (d_align(1), WB_Master.HWORD_HIGH_SEL, WB_Master.HWORD_LOW_SEL)
    }
    .elsewhen(d_type === CONSTANTS.LD_LB || d_type === CONSTANTS.LD_LBU){
       when(d_align === WB_Master.BYTE_3) {
         sel_vec := WB_Master.BYTE3_SEL
       }.elsewhen(d_align === WB_Master.BYTE_2){
         sel_vec := WB_Master.BYTE2_SEL
       }.elsewhen(d_align === WB_Master.BYTE_1){
         sel_vec := WB_Master.BYTE1_SEL
       }otherwise {
         sel_vec := WB_Master.BYTE0_SEL
       }
    }

  // connections between system MemIO bus and wbm
  io.wbm.addr_o := io.dmemIO.addr
  io.dmemIO.rdata := io.wbm.data_i
  io.wbm.data_o := io.dmemIO.wdata
  io.wbm.tgd_sttype_o := io.dmemIO.st_type
  io.wbm.we_o := io.dmemIO.wr_en
  io.wbm.sel_o := sel_vec // VecInit(true.B, true.B, true.B, true.B)
  io.wbm.stb_o := io.dmemIO.rd_en.toBool || io.dmemIO.wr_en.toBool
  io.wbm.cyc_o := true.B
  io.dmemIO.valid := io.wbm.ack_i
}

class WB_InterConnectIO extends Bundle {
  val dmemIO  = new DMemIO
  val imemIO  = new IMemIO
  val uartTx = Output(Bool())
  val uartRx = Input(Bool())
  val uartInt  = Output(Bool())
}

class WB_InterConnect(initFile: String)  extends Module with Config {
  val io = IO(new WB_InterConnectIO)
  val dmem = Module(new DataMem)
  val imem = Module(new InstMem(initFile))
  val wbmaster = Module(new WB_Master)
  val uart = Module(new UART)

  // Inst/Data Memory interface for Processor Core
  wbmaster.io.dmemIO <> io.dmemIO
  imem.io.imemIO <> io.imemIO
  // connections between wbm and wbs_dmem
  // dmem.io.wbs <> io.wbm

  // partial address decoding for the connected slaves
  val address = wbmaster.io.wbm.addr_o
  val imem_addr = address(WB_IMEM_ADDR_HIGH, WB_IMEM_ADDR_LOW) === WB_InterConnect.IMEM
  val dmem_addr = address(WB_DMEM_ADDR_HIGH, WB_DMEM_ADDR_LOW) === WB_InterConnect.DMEM
  val uart_addr = address(WB_UART_ADDR_HIGH, WB_UART_ADDR_LOW) === WB_InterConnect.UART


  // MT -- connect Data RAM to core using Wishbone shared bus interface
  dmem.io.wbs.addr_i := wbmaster.io.wbm.addr_o
  dmem.io.wbs.data_i := wbmaster.io.wbm.data_o
  dmem.io.wbs.we_i := wbmaster.io.wbm.we_o
  dmem.io.wbs.sel_i := wbmaster.io.wbm.sel_o
  dmem.io.wbs.stb_i := wbmaster.io.wbm.stb_o
  dmem.io.wbs.cyc_i := wbmaster.io.wbm.cyc_o
  dmem.io.wbs.tgd_sttype_i := wbmaster.io.wbm.tgd_sttype_o

  // MT -- connect Inst. Memory to core using Wishbone shared bus interface for reading constant and NOT instructions.
  // Instructions are read using dedicated IMemIO interface
  imem.io.wbs.addr_i := wbmaster.io.wbm.addr_o
  imem.io.wbs.data_i := wbmaster.io.wbm.data_o
  imem.io.wbs.we_i := wbmaster.io.wbm.we_o
  imem.io.wbs.sel_i := wbmaster.io.wbm.sel_o
  imem.io.wbs.stb_i := wbmaster.io.wbm.stb_o
  imem.io.wbs.cyc_i := wbmaster.io.wbm.cyc_o
  imem.io.wbs.tgd_sttype_i := wbmaster.io.wbm.tgd_sttype_o

  // Uart peripheral interface
  uart.io.wbs.addr_i := wbmaster.io.wbm.addr_o
  uart.io.wbs.data_i := wbmaster.io.wbm.data_o
  uart.io.wbs.we_i := wbmaster.io.wbm.we_o
  uart.io.wbs.sel_i := wbmaster.io.wbm.sel_o
  uart.io.wbs.stb_i := wbmaster.io.wbm.stb_o
  uart.io.wbs.cyc_i := wbmaster.io.wbm.cyc_o
  uart.io.wbs.tgd_sttype_i := wbmaster.io.wbm.tgd_sttype_o

  // MT -- Data read Mux when reading data from one of the slaves
  val imem_sel = Reg(Bool())
  val dmem_sel = Reg(Bool())
  val uart_sel = Reg(Bool())
  imem_sel := imem_addr && !imem.io.wbs.we_i && imem.io.wbs.stb_i
  dmem_sel := dmem_addr &&  dmem.io.wbs.stb_i   // MT  && !dmem.io.wbs.we_i
  uart_sel := uart_addr &&  uart.io.wbs.stb_i   // MT  && !uart.io.wbs.we_i

  wbmaster.io.wbm.data_i := Mux(dmem_sel, dmem.io.wbs.data_o,
                                Mux((imem_sel && !dmem.io.wbs.we_i) , imem.io.wbs.data_o,
                                    Mux((uart_sel && !uart.io.wbs.we_i), uart.io.wbs.data_o, 0.U)))
  wbmaster.io.wbm.ack_i := Mux(dmem_sel, dmem.io.wbs.ack_o,
                               Mux(imem_sel, imem.io.wbs.ack_o,
                                   Mux(uart_sel, uart.io.wbs.ack_o,  0.U)))


  // UART IO connectivity
  uart.io.uart_select := uart_addr
  uart.io.rxd := io.uartRx
  io.uartTx := uart.io.txd
  io.uartInt := uart.io.uartInt
}

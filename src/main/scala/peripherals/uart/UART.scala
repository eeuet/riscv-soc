// See LICENSE for license details.
package uart

import chisel3._
import chisel3.util._
import wishbone.{WB_InterConnect, WishboneSlaveIO}

trait UART_Config {
  val dataBits     = 8
  val stopBits     = 2
  val divisorBits  = 5
  val oversample   = 2
  val nSamples     = 3
  val nTxEntries   = 4
  val nRxEntries   = 4

  def oversampleFactor = 1 << oversample
  require(divisorBits > oversample)
  require(oversampleFactor > nSamples)
}

class UartIO extends Bundle {
  val uart_select = Input(Bool())
  val txd = Output(Bool())
  val rxd = Input(Bool())
  val uartInt = Output(Bool())

  // Include UART bus interface for Wishbone
  val wbs  = new WishboneSlaveIO
}

object UART {
  val UART_RXDATA_R  = 0x1.U
  val UART_TXDATA_R  = 0x2.U
  val UART_BAUD_R    = 0x3.U
  val UART_CONTROL_R = 0x4.U
  val UART_STATUS_R  = 0x5.U
  val UART_INT_MASK_R = 0x6.U
}

class UART extends Module with UART_Config { // (implicit p: Parameters)
  val io = IO(new UartIO)

  //  Instantiation for transmit and receive modules
  val txm = Module(new UARTTx)
  val rxm = Module(new UARTRx)

  // register definitions for IO and internal use
  val txen = RegInit(Bool(), false.B)
  val rxen = RegInit(Bool(), false.B)

  val tx_data_r  = RegInit(0x4A.U(dataBits.W))
  val rx_data_r  = RegInit(0x0.U(dataBits.W))
  val control_r  = RegInit(0x0.U(dataBits.W))
  val status_r   = RegInit(0x0.U(dataBits.W))
  val int_mask_r = RegInit(0x1.U(dataBits.W))

  val divisorInit = 8
  val div = RegInit(divisorInit.U(divisorBits.W)) // 8.U(c.divisorBits.W)

  private val stopCountBits = log2Up(stopBits)
  val nstop = RegInit(0.U(stopCountBits.W))

  // wiring of sub-modules and IOs
  txen := false.B
  txm.io.in.valid :=  txen
  txm.io.in.bits := (tx_data_r)
  txm.io.div := div
  txm.io.nstop := nstop
  rxm.io.div := div

  // IO wiring for Tx/Rx lines
  rxm.io.in := io.rxd
  io.txd := txm.io.out

  // Interface to Wishbone bus
  val addr = io.wbs.addr_i(3, 0)
  val rd_en = !io.wbs.we_i && io.wbs.stb_i && io.uart_select
  val wr_en = io.wbs.we_i && io.wbs.stb_i && io.uart_select
  val st_type = io.wbs.tgd_sttype_i

  // Wishbone handshake signals
  val ack = RegInit(Bool(), false.B)
  val rd_mux_sel = RegInit(Bool(), false.B)
  io.wbs.ack_o := ack
  ack := io.wbs.stb_i

  // Reading specific UART register on Wishbone bus
  val rd_data = RegInit(0.U(8.W))
  when(rd_en && addr === UART.UART_CONTROL_R){
    rd_data := control_r
  }
  .elsewhen(rd_en && addr === UART.UART_STATUS_R){
    rd_data := status_r
  }
  .elsewhen(rd_en && addr === UART.UART_RXDATA_R){
    rd_data := rx_data_r
  }otherwise {
    rd_data := 0.U
  }
  io.wbs.data_o := rd_data

  // UART interrupt generation
  io.uartInt := (status_r & int_mask_r).orR

  // UART register write operation on Wishbone bus
  when(wr_en) {
    when(addr === UART.UART_TXDATA_R) {
      tx_data_r := io.wbs.data_i(7, 0)
      txen := true.B
    }
    .elsewhen(addr === UART.UART_BAUD_R) {
      control_r := io.wbs.data_i(7, 0)
    }
    .elsewhen(addr === UART.UART_STATUS_R){
      status_r := io.wbs.data_i(7, 0)
    }
    .elsewhen(addr === UART.UART_CONTROL_R){
      control_r := io.wbs.data_i(7, 0)
    }
    .elsewhen(addr === UART.UART_INT_MASK_R){
      int_mask_r := io.wbs.data_i(7, 0)
    }
  }

  // Updating UART status register due to Tx/Rx activity
  when(rxm.io.out.valid){
    rx_data_r :=  (rxm.io.out.bits)
    status_r := Cat(status_r(7,1), true.B)  // Registering UART Rx interrupt
  }
  .elsewhen(txm.io.in.ready){
    status_r := status_r | 0x02.U          // Registering UART Tx interrupt
  }

  // internal loopback
  //  rxm.io.in := txm.io.out
}

// Instantiation of the UART module for Verilog generator
object Uart_generate extends App {

  chisel3.Driver.execute(args, () => new UART)
}

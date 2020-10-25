// See LICENSE for license details.
package uart

import Chisel._

case class UARTParams( dataBits: Int = 8,
                       stopBits: Int = 2,
                       divisorBits: Int = 5,
                       oversample: Int = 2,
                       nSamples: Int = 3,
                       nTxEntries: Int = 4,
                       nRxEntries: Int = 4)
{
  def oversampleFactor = 1 << oversample
  require(divisorBits > oversample)
  require(oversampleFactor > nSamples)
}

class UARTPortIO extends Bundle {
  val txd = Bool(OUTPUT)
  val rxd = Bool(INPUT)
  val en = Bool(INPUT)
  val rx_valid = Bool(OUTPUT)
}

/*
class UARTInterrupts extends Bundle {
  val rxwm = Bool()
  val txwm = Bool()
}
*/

class UART (c: UARTParams) extends Module  { // (implicit p: Parameters)
  val io = IO(new UARTPortIO)

  //  Instantiation for transmit and receive modules
  val txm = Module(new UARTTx(c))
  val rxm = Module(new UARTRx(c))

  // register definitions for IO and internal use
  val txen = Reg(init = Bool(false))
  val rxen = Reg(init = Bool(false))

  val tx_data = Reg(init = UInt(0x4A, (c.dataBits).W))
  val rx_data = Reg(init = UInt(0, (c.dataBits).W))

    val divisorInit = 8
    val div = Reg(init = UInt(divisorInit, c.divisorBits)) // 8.U(c.divisorBits.W)

    private val stopCountBits = log2Up(c.stopBits)
   // private val txCountBits = log2Floor(c.nTxEntries) + 1
   // private val rxCountBits = log2Floor(c.nRxEntries) + 1

    val nstop = Reg(init = UInt(0, stopCountBits))

  // wiring of sub-modules and IOs
    txm.io.en := io.en
     txm.io.in.bits := (tx_data)
    txm.io.div := div
    txm.io.nstop := nstop

    rxm.io.en := io.en
    rxm.io.div := div

    when (rxm.io.out.valid){
      rx_data :=  (rxm.io.out.bits)
    }

    io.txd :=  txm.io.out
    rxm.io.in := io.rxd
    io.rx_valid := rxm.io.out.valid

   // internal loopback
    rxm.io.in := txm.io.out
}

// Instantiation of the UART module for Verilog generator
object Uart_generate extends App {
  val c = UARTParams()
  chisel3.Driver.execute(args, () => new UART (c))
}

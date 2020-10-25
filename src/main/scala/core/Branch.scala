/********************************************************************
* Filename :    Branch.scala
* Date     :    28-03-2020
* 
* Description:  Derived from mini and made the area efficient Branch  
*               implementation as default.  
*********************************************************************/


package riscv_uet

import chisel3._
import CONSTANTS._

class BranchIO extends Bundle with Config {
  val rs1 = Input(UInt(XLEN.W))
  val rs2 = Input(UInt(XLEN.W))
  val br_type = Input(UInt(3.W))
  val taken = Output(Bool())
}

class Branch extends Module with Config {
  val io = IO(new BranchIO) 

  val diff = io.rs1 - io.rs2
  val neq  = diff.orR
  val eq   = !neq
  val isSameSign = io.rs1(XLEN-1) === io.rs2(XLEN-1)
  val lt   = Mux(isSameSign, diff(XLEN-1), io.rs1(XLEN-1))
  val ltu  = Mux(isSameSign, diff(XLEN-1), io.rs2(XLEN-1))
  val ge   = !lt
  val geu  = !ltu
  io.taken :=     
    ((io.br_type === BR_EQ) && eq) ||
    ((io.br_type === BR_NE) && neq) ||
    ((io.br_type === BR_LT) && lt) ||
    ((io.br_type === BR_GE) && ge) ||
    ((io.br_type === BR_LTU) && ltu) ||
    ((io.br_type === BR_GEU) && geu)
}



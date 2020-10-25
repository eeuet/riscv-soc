/******************************************************************************
* Filename :    Datapath.scala
* Date     :    28-03-2020
* 
* Description:  Based on riscv mini. Modified cache memory interfaces
*               to simple Inst/Data memory interfaces, resolved load-use
*               hazards by stalling and added support for external interrupts
*
* 5-05-2020    The load operation loads data in a temporary register 'ld_data'
*               at the start of cycle 3 and is loaded to register file at the
*               start of cycle 4. There is one cycle pipeline stall for load
*               instructions in case of load use hazard.
*******************************************************************************/


package riscv_uet

import chisel3._
import chisel3.util._

import CONSTANTS._
import CSR._

object Const {
  val PC_START   = 0x04
  val PC_EVEC    = 0x09  // vectored interrupt handling by default (mode = 1)
}

class DatapathIO extends Bundle {
  val debug      = new DebugIO
  val irq        = new IrqIO
  val imem       = Flipped(new IMemIO)
  val dmem       = Flipped(new DMemIO)
  val ctrl       = Flipped(new ControlSignals)
}

class Datapath extends Module with Config {
  val io         = IO(new DatapathIO)
  val csr        = Module(new CSR)
  val regFile    = Module(new RegFile)
  val alu        = Module(new ALU)
  val immGen     = Module(new ImmGen)
  val brCond     = Module(new Branch)


  /***** Fetch / Execute Registers *****/
  val fe_inst    = RegInit(Instructions.NOP)
  val fe_pc      = Reg(UInt())

  /***** Execute / Write Back Registers *****/
  val ew_inst = RegInit(Instructions.NOP) 
  val ew_pc      = Reg(UInt())
  val ew_alu     = Reg(UInt())
  val csr_in     = Reg(UInt())
  // val ld_data = Reg(UInt(XLEN.W))  // MT was used for Asynchronous data mem

  /****** Control signals *****/
  val st_type    = Reg(io.ctrl.st_type.cloneType)
  val ld_type    = Reg(io.ctrl.ld_type.cloneType)
  val wb_sel     = Reg(io.ctrl.wb_sel.cloneType)
  val wb_en      = Reg(Bool())
  val csr_cmd    = Reg(io.ctrl.csr_cmd.cloneType)
  val illegal    = Reg(Bool())
  val pc_check   = Reg(Bool())
 
  /****** Fetch *****/
  val notstarted = RegNext(reset.toBool)
  // MT stall is used to manage load-use hazard (for Load and CSR instructions)
  val stall      = Wire(Bool())
  val pc         = RegInit(Const.PC_START.U(XLEN.W) - 4.U(XLEN.W))
  val npc        = Mux(stall || !io.imem.inst_valid , pc, Mux(csr.io.expt, csr.io.evec,
                   Mux(io.ctrl.pc_sel === PC_EPC,  csr.io.epc,
                   Mux(io.ctrl.pc_sel === PC_ALU || brCond.io.taken, alu.io.sum >> 1.U << 1.U,
                   Mux(io.ctrl.pc_sel === PC_0, pc, pc + 4.U)))))

  // MT -- verify the use of "io.imem.inst_valid" here to insert NOP
  val inst       = Mux(notstarted || io.ctrl.inst_kill || brCond.io.taken || csr.io.expt || !io.imem.inst_valid,
                       Instructions.NOP, io.imem.inst)
  pc             := npc
  io.imem.addr   := npc  // address for next instruction fetch from instruction memory

 
  // Pipelining
  // updating fetch-execute pipeline registers
  when (!stall) {
    fe_pc        := pc
    fe_inst      := inst
  }

  /****** Execute *****/
  // Decode
  io.ctrl.inst   := fe_inst

  // regFile read
  val rd_addr    = fe_inst(11, 7)
  val rs1_addr   = fe_inst(19, 15)
  val rs2_addr   = fe_inst(24, 20)
  regFile.io.raddr1 := rs1_addr
  regFile.io.raddr2 := rs2_addr

  // gen immediate
  immGen.io.inst := fe_inst
  immGen.io.sel  := io.ctrl.imm_sel

  // hazard detection
  val wrbk_rd_addr = ew_inst(11, 7)
  val rs1hazard  = wb_en && rs1_addr.orR && (rs1_addr === wrbk_rd_addr)
  val rs2hazard  = wb_en && rs2_addr.orR && (rs2_addr === wrbk_rd_addr)
  // forwarding to resolve RAW hazard
  val rs1        = Mux(wb_sel === WB_ALU && rs1hazard, ew_alu, regFile.io.rdata1)
  val rs2        = Mux(wb_sel === WB_ALU && rs2hazard, ew_alu, regFile.io.rdata2)

  // MT -- load-use and CSRs hazard detection. The pipeline is stalled since the hazard can not be resolved using
  // forwarding/bypassing
  stall          := (ld_type.orR || csr_cmd =/= CSR.Z) && ((io.ctrl.en_rs1 && rs1hazard) || (io.ctrl.en_rs2 && rs2hazard))
 
  // ALU operations
  alu.io.in_A    := Mux(io.ctrl.A_sel === A_RS1, rs1, fe_pc)
  alu.io.in_B    := Mux(io.ctrl.B_sel === B_RS2, rs2, immGen.io.out)
  alu.io.alu_Op  := io.ctrl.alu_op

  // Branch condition calculation
  brCond.io.rs1  := rs1
  brCond.io.rs2  := rs2
  brCond.io.br_type := io.ctrl.br_type

  // Data memory access
  io.dmem.addr   := alu.io.sum
  
  io.dmem.wr_en   := Mux(stall, ST_XXX.orR, io.ctrl.st_type.orR)  // MT -- stall for Load-use hazard
  io.dmem.st_type := io.ctrl.st_type
  io.dmem.wdata   := MuxLookup(io.ctrl.st_type, 0.U ,
                               Seq(    // MT -- have used default value of 0.U should be reconsidered
                                 ST_SW -> rs2 ,
                                 ST_SH -> rs2(15, 0) ,
                                 ST_SB -> rs2(7, 0) )).asUInt

  // MT Pipelining control signals updation for exception/interrupt, stall and normal conditions
  // MT -- TO DO -- what if we have stall and exception simultaneously?
  when(reset.toBool || !stall && csr.io.expt) {
    st_type      := 0.U
    ld_type      := 0.U
    wb_en        := false.B
    csr_cmd      := 0.U
    illegal      := false.B
    pc_check     := false.B
  }.elsewhen(!stall && !csr.io.expt) {
    // updating execute-writeback pipeline registers
    ew_pc        := fe_pc
    ew_inst      := fe_inst
    ew_alu       := alu.io.out
    // updating control signals for writeback stage
    csr_in       := Mux(io.ctrl.imm_sel === IMM_Z, immGen.io.out, rs1)
    st_type      := io.ctrl.st_type
    ld_type      := io.ctrl.ld_type
    wb_sel       := io.ctrl.wb_sel
    wb_en        := io.ctrl.wb_en
    csr_cmd      := io.ctrl.csr_cmd
    illegal      := io.ctrl.illegal
    pc_check     := io.ctrl.pc_sel === PC_ALU
  }.elsewhen(stall && !csr.io.expt) {
    // MT clearing wirteback signals to stop stalled instruction from updating register-files and memory, rather
    // they will be updated in the next cycle with correct data from the preceding instruction (which was the
    // source of stalling)
    st_type      := ST_XXX
    ld_type      := LD_XXX
    wb_en        := false.B
    csr_cmd      := CSR.Z
  }

  //  data memory load operation
  io.dmem.rd_en   := io.ctrl.ld_type.orR
  io.dmem.ld_type := io.ctrl.ld_type

  // MT -- read valid is asserted on receiving ack_i from dmem (wishbone slave i.e. io.dmem.valid),
  // when a read operation (ld_type.orR) was performed
  val rd_valid    = io.dmem.valid && ld_type.orR
  val ld_data     = Mux(rd_valid, io.dmem.rdata, 0.U)
  val load        = MuxLookup(ld_type, io.dmem.rdata.zext, Seq(
                             LD_LW  -> ld_data.zext,
                             LD_LH  -> ld_data(15, 0).asSInt, LD_LB  -> ld_data(7, 0).asSInt,
                             LD_LHU -> ld_data(15, 0).zext,   LD_LBU -> ld_data(7, 0).zext) )

  // CSR access
  csr.io.stall     := stall
  csr.io.in        := csr_in
  csr.io.cmd       := csr_cmd
  csr.io.inst      := ew_inst
  csr.io.pc        := ew_pc
  csr.io.addr      := ew_alu
  csr.io.illegal   := illegal
  csr.io.pc_check  := pc_check
  csr.io.ld_type   := ld_type
  csr.io.st_type   := st_type
  // MT Added support for external interrupts (IRQs)
  csr.io.irq       <> io.irq

  // Regfile Write
  val regWrite = MuxLookup(wb_sel, ew_alu.zext,
                           Seq( WB_MEM -> load,
                                WB_PC4 -> (ew_pc + 4.U).zext,
                                WB_CSR -> csr.io.out.zext) ).asUInt

  regFile.io.wen   := wb_en && !csr.io.expt
  regFile.io.waddr := wrbk_rd_addr
  regFile.io.wdata := regWrite

/* MT  if (p(Trace)) {
    printf("PC: %x, INST: %x, REG[%d] <- %x\n", ew_pc, ew_inst,
      Mux(regFile.io.wen, wb_rd_addr, 0.U),
      Mux(regFile.io.wen, regFile.io.wdata, 0.U))
  } */

 // MT -- debug signals defined temporarily, will be finalized later
  io.debug.inst     := fe_inst
  io.debug.regWaddr := wrbk_rd_addr
  io.debug.regWdata := regWrite
  io.debug.pc       := fe_pc
}

object Datapath_generate extends App {
  chisel3.Driver.execute(args, () => new Datapath )
} 



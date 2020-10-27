## RISCV_UET  
This is a simple RISC-V 3-stage pipeline processor based on riscv mini from UCB and currently implements RV32I ISA based on User-level ISA Version 2.0, (update to 2.2 is required). Its a 3 stage pipelined processor, with no structural hazards, no branch prediction and Data Hazards are resolved using forwarding and stalling.

 
Following updates have been incorporated in the current implementation:

- Machine-level ISA has been upgraded to Privileged Architecture Version 1.11. Currently only supports Machine Mode.
- Support for machine level interrupts has been added. It includes the support for vectored interrupts.
- External interrupts are supported using bits 16 and above of MIP & MIE CSRs as provisioned by Privileged Architecture Version 1.11. 
- Resolved Load-Use hazard. 
- Simple instruction and data memory integration (Support for both Asynchronous and Synchronous Reads has been implemented.).

Work in Progress:

- Support for peripheral interfaces using Wishbone
- Integration of memory mapped DSP accelerator

### Generating Verilog
Different components for the processor core are integrated in `'src/main/scala/ProcessorTile.scala'`. The verilog code can be generated by executing the following command:

`> sbt run`
 
and choosing 

`riscv_uet.Core_Main` 

// See LICENSE for license details.

package librefpga

import chisel3._
import chisel3.util.Cat

class LUT4 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(4.W))
    val out = Output(UInt(1.W))

    val cfg_sen = Input(Bool())
    val cfg_sin = Input(UInt(1.W))
    val cfg_sout = Output(UInt(1.W))
  })

  val sreg = Reg(init = 0.U(16.W))
  
  io.cfg_sout := sreg(0)

  when(io.cfg_sen) {
    sreg := Cat(io.cfg_sin, sreg(15,1))
  }

  io.out := sreg(io.in)

}

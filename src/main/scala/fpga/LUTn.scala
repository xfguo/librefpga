// See LICENSE for license details.

package librefpga

import chisel3._
import chisel3.util.Cat

class ConfigIo extends Bundle {
  val sen = Input(Bool())
  val sin = Input(UInt(1.W))
  val sout = Output(UInt(1.W))
}

class LogicIo extends Bundle {
  val lutx = 4
  val in = Input(UInt(lutx.W))
  val out = Output(UInt(1.W))
}

class LUTn extends Module {
  val lutx = 4
  val io = IO(new Bundle {
    val logic = new LogicIo
    val cfg = new ConfigIo
  })

  val sreg = Reg(init = 0.U((1 << lutx).W))
  
  io.cfg.sout := sreg(0)

  when(io.cfg.sen) {
    sreg := Cat(io.cfg.sin, sreg(((1 << lutx) - 1),1))
  }

  io.logic.out := sreg(io.logic.in)
}

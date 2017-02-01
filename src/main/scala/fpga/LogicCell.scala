// See LICENSE for license details.

package librefpga

import chisel3._
import chisel3.util._

import librefpga._

/*
 * config:
 *   | LUTn         | 2^lutn - 1 + 4:4 | LUTn input                  |
 *   | CARRY        |           3:2 | 0: in[2]; 1: fcin; 2: in[1] |
 *   | LUT[3]_FCIN  |             1 | 0: in[3]; 1: fcin;          |
 *   | OUT_SEL      |             0 | 0: lutn_out; 1: dff_out     |
 */

class CarryIo extends Bundle {
  val in = Input(UInt(1.W))
  val out = Output(UInt(1.W))
}

class LogicCellIo extends Bundle {
  val logic = new LogicIo
  val carry = new CarryIo
  val cfg = new ConfigIo
}

class LogicCell extends Module {
  val lutx = 4
  val io = IO(new LogicCellIo)

  val dff = Reg(init = 0.U(1.W))

  val sreg = Reg(init = 0.U(lutx.W))

  val out_sel = sreg(0).asUInt
  val lut3_fcin_sel = sreg(1).asUInt
  val carry_sel = sreg(3, 2).asUInt

  val lutn = Module(new LUTn)
  
  io.logic.out := sreg(io.logic.in)

  io.carry.out := MuxLookup(carry_sel, 0.U, 
               Array(
                 0.U -> io.logic.in(2),
                 1.U -> io.carry.in,
                 2.U -> io.logic.in(1)
               )
             )
  /* out output sel: 0: LUTn out, 1: DFF */
  io.logic.out := Mux(out_sel === 0.U, lutn.io.logic.out, dff)

  lutn.io.cfg.sen := io.cfg.sen
  lutn.io.logic.in := Cat(
    Mux(lut3_fcin_sel === 1.U, io.carry.in, io.logic.in(3)),
    io.logic.in(2, 0))
  
  io.cfg.sout := sreg(0)

  when(io.cfg.sen) {
     sreg := Cat(lutn.io.cfg.sout, sreg(3, 1))
  }
  
  lutn.io.cfg.sin := io.cfg.sin

  /* DFF */
  dff := lutn.io.logic.out

}

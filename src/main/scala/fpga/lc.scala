// See LICENSE for license details.

package librefpga

import chisel3._
import chisel3.util._

import librefpga.LUT4

/*
 * config:
 *   | CARRY        | 19:18 | 0: in[2]; 1: fcin; 2: in[1] |
 *   | LUT[3]_FCIN  |    17 | 0: in[3]; 1: fcin;          |
 *   | OUT_SEL      |    16 | 0: lut4_out; 1: dff_out     |
 *   | LUT4         | 15: 0 | LUT4 input                  |
 */

class LC extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(4.W))
    val out = Output(UInt(1.W))
    
    val fcin = Input(UInt(1.W))
    val fcout = Output(UInt(1.W))

    val cfg_sen = Input(Bool())
    val cfg_sin = Input(UInt(1.W))
    val cfg_sout = Output(UInt(1.W))
  })

  val dff = Reg(init = 0.U(1.W))

  val sreg = Reg(init = 0.U(4.W))

  val out_sel = sreg(0).asUInt
  val lut3_fcin_sel = sreg(1).asUInt
  val carry_sel = sreg(3, 2).asUInt

  val lut4 = Module(new LUT4)
  
  io.out := sreg(io.in)

  io.fcout := MuxLookup(carry_sel, 0.U, 
               Array(
                 0.U -> io.in(2),
                 1.U -> io.fcin,
                 2.U -> io.in(1)
               )
             )
  /* out output sel: 0: LUT4 out, 1: DFF */
  io.out := Mux(out_sel === 0.U, lut4.io.out, dff)

  lut4.io.cfg_sen := io.cfg_sen
  lut4.io.in := Cat(
    Mux(lut3_fcin_sel === 1.U, io.fcin, io.in(3)),
    io.in(2, 0))
  
  io.cfg_sout := lut4.io.cfg_sout

  when(io.cfg_sen) {
     sreg := Cat(io.cfg_sin, sreg(3, 1))
  }

  dff := lut4.io.out
  
  lut4.io.cfg_sin := sreg(0)

}

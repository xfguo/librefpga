// See LICENSE for license details.

package librefpga

import chisel3._
import chisel3.util._

import librefpga._
import librefpga.util._

class InterconnectIo extends Bundle {
  val plbio = Flipped(new PLBIo)
  val cfg = new ConfigIo
  val ins = Vec(2 * 4, Input(UInt(1.W)))
  val outs = Vec(4, Output(UInt(1.W)))
}

/*
 * config:
 *  | name                | size                          |
 *  | ------------------- | ------------------------------|
 *  | (in) input select   | le_num * lutx * in_sel_width  |
 *  | output select (out) | out_num * out_sel_width       |
 */
class Interconnect extends Module {
  val lutx = 4 /* input number of LUT */
  val le_num = 8 /* number of logic elements in PLB */
  val carry_in_num = 1 /* carry in from PLB */
  val carry_out_num = 1 /* carry out from PLB */
  
  val in_sel_width = UInt(2 * 4 - 1).getWidth /* bits of number mux select number (with carry in) */

  val out_num = 4 /* output wires number */
  val out_sel_width = UInt(le_num - 1 + carry_out_num).getWidth /* bits of output mux select number (with carry out) */

  val io = IO(new InterconnectIo)

  val out_sel_sreg = Reg(UInt((out_num * out_sel_width).W)) /* lut out & carry out sels */
  val in_sel_sreg = Reg(UInt(((le_num * lutx + 1) * in_sel_width).W)) /* lut in & carry in sels */

  /* input mux: io port => PLB in (lut and carry) */
  val in_mux = Module(new MuxMtoN(2 * 4, le_num * lutx + carry_in_num))

  in_mux.io.ins := io.ins

  for (i <- 0 to le_num - 1) {
    io.plbio.logics(i).in := Cat( (0 to lutx - 1).map(x => in_mux.io.outs( x + i * lutx ) ).reverse )
  }

  io.plbio.carry.in := in_mux.io.outs(le_num * lutx)
  
  for (i <- 0 to le_num * lutx + carry_in_num - 1) {
    in_mux.io.sels(i) := in_sel_sreg((i + 1) * in_sel_width - 1, i * in_sel_width)
  }
  
  /* output mux: PLB output (lut and carry) => io port */
  val out_mux = Module(new MuxMtoN(le_num + carry_out_num, out_num))

  io.outs := out_mux.io.outs

  for (i <- 0 to le_num - 1) {
    out_mux.io.ins(i) := io.plbio.logics(i).out
  }

  out_mux.io.ins(le_num) := io.plbio.carry.out

  for (i <- 0 to out_num - 1) {
    out_mux.io.sels(i) := out_sel_sreg((i + 1) * out_sel_width - 1, i * out_sel_width)
  }

  /* config */
  io.cfg.sout := out_sel_sreg(0)

  when(io.cfg.sen) {
    in_sel_sreg := Cat(io.cfg.sin, in_sel_sreg(((le_num * lutx + carry_in_num) * in_sel_width) - 1, 1))
    out_sel_sreg := Cat(in_sel_sreg(0), out_sel_sreg((out_num * out_sel_width) - 1, 1))
  }
}

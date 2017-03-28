// See LICENSE for license details.

package librefpga

import chisel3._
import chisel3.util._

import librefpga._
import librefpga.util._

class LogicTileIo extends Bundle {
  val ins = Vec(2 * 4, Input(UInt(1.W)))
  val outs = Vec(4, Output(UInt(1.W)))
  val cfg = new ConfigIo()
}

class LogicTile extends Module {
  val io = IO(new LogicTileIo)

  val plb = Module(new PLB)
  val ic = Module(new Interconnect)

  ic.io.ins := io.ins
  io.outs := ic.io.outs

  plb.io <> ic.io.plbio
  
  /* config */
  io.cfg.sout := plb.io.cfg.sout
  plb.io.cfg.sin := ic.io.cfg.sout
  ic.io.cfg.sin := io.cfg.sin

  plb.io.cfg.sen := io.cfg.sen
  ic.io.cfg.sen := io.cfg.sen

}

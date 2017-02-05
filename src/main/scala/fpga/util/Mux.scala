// See LICENSE for license details.

package librefpga.util

import chisel3._
import chisel3.util._

import librefpga._

class MuxMtoN(m: Int, n:Int) extends Module {
  val w = UInt(m - 1).getWidth

  val io = IO(new Bundle {
    val sels = Vec(n, Input(UInt(w.W)))
    val ins = Vec(m, Input(UInt(1.W)))
    val outs = Vec(n, Output(UInt(1.W)))
  })

  (io.outs zip io.sels).foreach{ case (o, s) => o := io.ins(s) }
}



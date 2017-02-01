// See LICENSE for license details.

package librefpga

import chisel3._
import chisel3.util._

import librefpga._

class PLBIo extends Bundle {
  val n = 8
  val logics = Vec(n, new LogicIo)
  val carry = new CarryIo()
  val cfg = new ConfigIo()
}

class PLB extends Module {
  val lutx = 4
  val n = 8
  val io = IO(new PLBIo)
  
  val max = n - 1

  /* logic cells */
  val LCs = Vec.fill(n)(Module(new LogicCell).io)

  for (i <- 0 to n - 1)
    io.logics(i) <> LCs(i).logic

  /* carry chain
   *
   * Modul.carry.in  -> LC[0].carry.in
   * LC[0].carry.out -> LC[1].carry.in
   * LC[1].carry.out -> LC[2].carry.in
   * ...
   * LC[6].carry.out -> LC[7].carry.in
   * LC[7].carry.out -> Modul.carry.out 
   */
  LCs(0).carry.in := io.carry.in
  for (i <- 0 to (max - 1)) {
    LCs(i + 1).carry.in := LCs(i).carry.out
  }
  io.carry.out := LCs(max).carry.out

  /* config chain */
  LCs(max).cfg.sin := io.cfg.sin
  for (i <- 1 to max) {
    LCs(i).cfg.sin := LCs(i - 1).cfg.sout
  }
  io.cfg.sout := LCs(0).cfg.sout
  
  for (i <- 0 to max)
    LCs(i).cfg.sen := io.cfg.sen
}

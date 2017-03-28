// See LICENSE for license details.

package librefpga.test

import Chisel.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import librefpga.LogicTile

import chisel3._
import chisel3.util._

class LogicTileUnitTester(c: LogicTile) extends PeekPokeTester(c) {
  private val lt = c

  /* TODO */
}

class LogicTileTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for ( backendName <- backendNames ) {
    "LogicTile" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new LogicTile, backendName) {
        c => new LogicTileUnitTester(c)
      } should be (true)
    }
  }
}

// See LICENSE for license details.

package librefpga.test

import Chisel.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import librefpga.Interconnect

import chisel3._
import chisel3.util._

class InterconnectUnitTester(c: Interconnect) extends PeekPokeTester(c) {
  private val ic = c

  val lutx = 4
  val n = 8

}

class InterconnectTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for ( backendName <- backendNames ) {
    "Interconnect" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new Interconnect, backendName) {
        c => new InterconnectUnitTester(c)
      } should be (true)
    }
  }
}

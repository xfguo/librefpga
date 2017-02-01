// See LICENSE for license details.

package librefpga.test

import Chisel.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import librefpga.PLB

import chisel3._
import chisel3.util._

class PLBUnitTester(c: PLB) extends PeekPokeTester(c) {
  private val plb = c

  val lutx = 4
  val n = 8

}

class PLBTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for ( backendName <- backendNames ) {
    "PLB" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new PLB, backendName) {
        c => new PLBUnitTester(c)
      } should be (true)
    }
  }
}

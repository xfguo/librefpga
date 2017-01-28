// See LICENSE for license details.

package librefpga.test

import Chisel.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import librefpga.LUT4

import chisel3._

class LUT4UnitTester(c: LUT4) extends PeekPokeTester(c) {
  private val lut4 = c

  poke(lut4.io.cfg_sen, 0)

  step(1)

  /* test default outputs (0) */
  expect(lut4.io.out, 0)
  expect(lut4.io.cfg_sout, 0)

  for(i <- 0 to 15) {
    poke(lut4.io.in, i)
    step(1)
    expect(lut4.io.out, 0)
  }
  
  val rnd_gen = new scala.util.Random()

  /* generate random test pattern to test look-up table */
  for(i <- 0 to 100) {
    val rnum = UInt(rnd_gen.nextInt(65536))

    /* shift it in */
    for (j <- 0 to 15) {
      poke(lut4.io.cfg_sen, true)
      poke(lut4.io.cfg_sin, rnum(j))
      step(1)
    }

    poke(lut4.io.cfg_sen, false)
    step(1)

    /* test output */
    for (j <- 0 to 15) {
      poke(lut4.io.in, j)
      step(1)
      expect(lut4.io.out, rnum(j))
    }
  }
}

class LUT4Tester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for ( backendName <- backendNames ) {
    "LUT4" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new LUT4, backendName) {
        c => new LUT4UnitTester(c)
      } should be (true)
    }
  }
}

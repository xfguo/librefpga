// See LICENSE for license details.

package librefpga.test

import Chisel.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import librefpga.LUTn

import chisel3._

class LUTnUnitTester(c: LUTn) extends PeekPokeTester(c) {
  private val lutn = c

  poke(lutn.io.cfg.sen, 0)

  step(1)

  /* test default outputs (0) */
  expect(lutn.io.logic.out, 0)
  expect(lutn.io.cfg.sout, 0)

  for(i <- 0 to 15) {
    poke(lutn.io.logic.in, i)
    step(1)
    expect(lutn.io.logic.out, 0)
  }
  
  val rnd_gen = new scala.util.Random()

  /* generate random test pattern to test look-up table */
  for(i <- 0 to 100) {
    val rnum = UInt(rnd_gen.nextInt(65536))

    /* shift it in */
    for (j <- 0 to 15) {
      poke(lutn.io.cfg.sen, true)
      poke(lutn.io.cfg.sin, rnum(j))
      step(1)
    }

    poke(lutn.io.cfg.sen, false)
    step(1)

    /* test output */
    for (j <- 0 to 15) {
      poke(lutn.io.logic.in, j)
      step(1)
      expect(lutn.io.logic.out, rnum(j))
    }
  }
}

class LUTnTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for ( backendName <- backendNames ) {
    "LUTn" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new LUTn, backendName) {
        c => new LUTnUnitTester(c)
      } should be (true)
    }
  }
}

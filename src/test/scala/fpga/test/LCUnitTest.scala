// See LICENSE for license details.

package librefpga.test

import Chisel.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import librefpga.LC

import chisel3._

class LCUnitTester(c: LC) extends PeekPokeTester(c) {
  private val lc = c

  poke(lc.io.cfg_sen, 0)

  step(1)

  /* test default outputs (0) */
  expect(lc.io.out, 0)
  expect(lc.io.cfg_sout, 0)

  /* default config in LC: LUT4 input === LC input */
  for(i <- 0 to 15) {
    poke(lc.io.in, i)
    step(1)
    expect(lc.io.out, 0)
  }
  
  val rnd_gen = new scala.util.Random()

  /* generate random test pattern to test look-up table */

  for(i <- 0 to 10000) {
    val rand_cfg_num = rnd_gen.nextInt(0xFFFFF)
    val rand_cfg = UInt(rand_cfg_num)

    /* shift lut4 config */
    for (j <- 0 to 19) {
      poke(lc.io.cfg_sen, true)
      poke(lc.io.cfg_sin, rand_cfg(j))
      step(1)
    }
    
    poke(lc.io.cfg_sen, false)
    step(1)

    /* test output */
    for (fcin <- 0 to 1) {
      if ((rand_cfg_num & (1 << 16)) != 0) {
        /* latch with DFF */
  
        poke(lc.io.fcin, fcin)

        for (j <- 0 to 15) {
          poke(lc.io.in, j)
          step(2)
          if ((rand_cfg_num & (1 << 17)) != 0) {
            expect(lc.io.out, rand_cfg((j & 7) | (fcin << 3)))
  	  } else {
              expect(lc.io.out, rand_cfg(j))
  	  }
  
  	  val carry_sel = ((rand_cfg_num >> 18) & 0x3)
  	  if (carry_sel == 0)
  	    expect(lc.io.fcout, (j >> 2) & 1)
  	  else if (carry_sel == 1)
  	    expect(lc.io.fcout, fcin)
  	  else if (carry_sel == 2)
  	    expect(lc.io.fcout, (j >> 1) & 1)
  	  else
  	    expect(lc.io.fcout, 0)
        }
        
      } else {
        /* direct output from lut4 */
	poke(lc.io.fcin, fcin)

        for (j <- 0 to 15) {
          poke(lc.io.in, j)
          step(1)
          if ((rand_cfg_num & (1 << 17)) != 0) {
            expect(lc.io.out, rand_cfg((j & 7) | (fcin << 3)))
  	  } else {
              expect(lc.io.out, rand_cfg(j))
  	  }

  	  val carry_sel = ((rand_cfg_num >> 18) & 0x3)
  	  if (carry_sel == 0)
  	    expect(lc.io.fcout, (j >> 2) & 1)
  	  else if (carry_sel == 1)
  	    expect(lc.io.fcout, fcin)
  	  else if (carry_sel == 2)
  	    expect(lc.io.fcout, (j >> 1) & 1)
  	  else
  	    expect(lc.io.fcout, 0)
        }
      }
    }
  }
}

class LCTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for ( backendName <- backendNames ) {
    "LC" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new LC, backendName) {
        c => new LCUnitTester(c)
      } should be (true)
    }
  }
}

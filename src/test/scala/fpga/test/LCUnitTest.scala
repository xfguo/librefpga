// See LICENSE for license details.

package librefpga

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3._

class LogicCellUnitTester(c: LogicCell) extends PeekPokeTester(c) {
  private val lc = c

  val lutx = 4
  val cfg_bits_n = (1 << lutx) + 4

  poke(lc.io.cfg.sen, 0)

  step(1)

  /* test default outputs (0) */
  expect(lc.io.logic.out, 0)
  expect(lc.io.cfg.sout, 0)

  /* default config in LogicCell: LUTn input === LogicCell input */
  for(i <- 0 to ((1 << lutx) - 1)) {
    poke(lc.io.logic.in, i)
    step(1)
    expect(lc.io.logic.out, 0)
  }
  
  val rnd_gen = new scala.util.Random()

  /* generate random test pattern to test look-up table */

  for(i <- 0 to 200) {
    val rand_cfg_num = rnd_gen.nextInt((1 << cfg_bits_n) - 1)
    val rand_cfg = UInt(rand_cfg_num)
    val carry_sel = ((rand_cfg_num >> 2) & 0x3)

    /* shift lutn config */
    for (j <- 0 to (cfg_bits_n - 1)) {
      poke(lc.io.cfg.sen, true)
      poke(lc.io.cfg.sin, rand_cfg(j))
      step(1)
    }
    
    poke(lc.io.cfg.sen, false)
    step(1)

    /* test output */
    for (carry_in <- 0 to 1) {
      if ((rand_cfg_num & (1 << 0)) != 0) {
        /* test latch with DFF */
        poke(lc.io.carry.in, carry_in)

        for (j <- 0 to ((1 << lutx) - 1)) {
          poke(lc.io.logic.in, j)
          step(2)
          if ((rand_cfg_num & (1 << 1)) != 0) {
            expect(lc.io.logic.out, rand_cfg( ((j & 7) | (carry_in << 3)) + 4))
  	  } else {
            expect(lc.io.logic.out, rand_cfg(j + 4))
  	  }
  
  	  if (carry_sel == 0)
  	    expect(lc.io.carry.out, (j >> 2) & 1)
  	  else if (carry_sel == 1)
  	    expect(lc.io.carry.out, carry_in)
  	  else if (carry_sel == 2)
  	    expect(lc.io.carry.out, (j >> 1) & 1)
  	  else
  	    expect(lc.io.carry.out, 0)
        }
        
      } else {
        /* test direct output from lut4 */
	poke(lc.io.carry.in, carry_in)

        for (j <- 0 to ((1 << lutx) - 1)) {
          poke(lc.io.logic.in, j)
          step(1)
          if ((rand_cfg_num & (1 << 1)) != 0) {
            expect(lc.io.logic.out, rand_cfg( ((j & 7) | (carry_in << 3)) + 4))
  	  } else {
            expect(lc.io.logic.out, rand_cfg(j + 4))
  	  }

  	  if (carry_sel == 0)
  	    expect(lc.io.carry.out, (j >> 2) & 1)
  	  else if (carry_sel == 1)
  	    expect(lc.io.carry.out, carry_in)
  	  else if (carry_sel == 2)
  	    expect(lc.io.carry.out, (j >> 1) & 1)
  	  else
  	    expect(lc.io.carry.out, 0)
        }
      }
    }
  }
}

class LogicCellTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for ( backendName <- backendNames ) {
    "LogicCell" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new LogicCell, backendName) {
        c => new LogicCellUnitTester(c)
      } should be (true)
    }
  }
}

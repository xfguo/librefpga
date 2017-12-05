// See LICENSE for license details.

package librefpga

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3._
import chisel3.util._

class PLBUnitTester(c: PLB) extends PeekPokeTester(c) {
  private val plb = c

  val lutx = 4
  val n = 8

  val cfg_bits_lut_size = 1 << lutx
  val cfg_bits_per_le = cfg_bits_lut_size + 4
  val cfg_bits_total = cfg_bits_per_le * n

  poke(plb.io.carry.in, 0)
  poke(plb.io.cfg.sen, 0)

  step(1)

  /* test default outputs (0) */
  expect(plb.io.cfg.sout, 0)

  /* default config */
  for(i <- 0 to (n - 1)) {
    for (j <- 0 to ((1 << lutx) - 1)) {
      poke(plb.io.logics(i).in, j)
      step(1)
      expect(plb.io.logics(i).out, 0)
    }
  }

  val rnd_gen = new scala.util.Random()

  /* generate random test pattern to test look-up table */

  for(i <- 0 to 100) {
    val rand_cfg = Array.fill(n)(rnd_gen.nextInt((1 << cfg_bits_per_le) - 1))
    val rand_lut_inputs = Array.fill(n)(rnd_gen.nextInt((1 << lutx) - 1))
    val rand_carry_in = rnd_gen.nextInt(1)
   
    if (false) {
      for (k <- 0 to n - 1) {
        val rc = rand_cfg(k)
        println(f"$k%02x: rand_cfg=$rc%04x")
      }
      
      for (k <- 0 to n - 1) {
        val ri = rand_lut_inputs(k)
        println(f"$k%02x: rand_input =$ri%x")
      }
      
      println(f"carryin=$rand_carry_in")
    }
    
    /* shift config in */
    for (k <- 0 to n - 1) {
      for (j <- 0 to cfg_bits_per_le - 1) {
        poke(plb.io.cfg.sen, true)
        poke(plb.io.cfg.sin, (rand_cfg(k) >> j) & 1)
        step(1)
      }
    }
    
    poke(plb.io.cfg.sen, false)
    step(1)

    /* logic and carry input */

    poke(plb.io.carry.in, rand_carry_in)
    for (k <- 0 to n - 1) {
      poke(plb.io.logics(k).in, rand_lut_inputs(k).asUInt(4.W))
    }
    step(1)

    /* calcuate lut & carry_out */
    val expect_carry_outs = Array.fill(n + 1)(0)
    val expect_lut_outs = Array.fill(n)(0)
    val dff_sels = Array.fill(n)(0)
 
    expect_carry_outs(0) = rand_carry_in
    for (k <- 0 to n - 1) {
      val carry_sel = (rand_cfg(k) >> 2) & 0x3
      val lut3_fcin_sel = rand_cfg(k) & (1 << 1)
      
      dff_sels(k) = rand_cfg(k) & (1 << 0)

      if (lut3_fcin_sel != 0) {
        expect_lut_outs(k) = (rand_cfg(k) >> (((rand_lut_inputs(k) & 7) | (expect_carry_outs(k) << 3)) + 4)) & 1
      } else {
        expect_lut_outs(k) = (rand_cfg(k) >> (rand_lut_inputs(k) + 4)) & 1
      }
  
      if (carry_sel == 0)
        expect_carry_outs(k + 1) = (rand_lut_inputs(k) >> 2) & 1
      else if (carry_sel == 1)
        expect_carry_outs(k + 1) = expect_carry_outs(k)
      else if (carry_sel == 2)
        expect_carry_outs(k + 1) = (rand_lut_inputs(k) >> 1) & 1
      else
        expect_carry_outs(k + 1) = 0
    }


    /* comparation */
    step(1)

    expect(plb.io.carry.out, expect_carry_outs(n))
    for (k <- 0 to n - 1) {
      if (dff_sels(k) == 0) {
        expect(plb.io.logics(k).out, expect_lut_outs(k))
      }
    }
    
    step(1) /* after dff */

    for (k <- 0 to n - 1) {
      if (dff_sels(k) == 1) {
        expect(plb.io.logics(k).out, expect_lut_outs(k))
      }
    }
  }
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

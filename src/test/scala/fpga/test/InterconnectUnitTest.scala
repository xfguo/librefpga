// See LICENSE for license details.

package librefpga.test

import Chisel.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import librefpga.Interconnect

import chisel3._
import chisel3.util._

import scala.util.Random

class InterconnectUnitTester(c: Interconnect) extends PeekPokeTester(c) {
  private val ic = c

  val lutx = 4 /* input number of LUT */
  val le_num = 8 /* number of logic elements in PLB */
  val carry_in_num = 1 /* carry in from PLB */
  val carry_out_num = 1 /* carry out from PLB */
  
  val in_sel_width = UInt(2 * 4 - 1 + carry_in_num).getWidth /* bits of number mux select number (with carry in) */

  val out_num = 4 /* output wires number */
  val out_sel_width = UInt(le_num - 1 + carry_out_num).getWidth /* bits of output mux select number (with carry out) */
  
  val rnd_gen = new Random()
  

  for (n <- 0 to 0) {
    val in_sel_pat = Array.fill(le_num * lutx + carry_in_num) { rnd_gen.nextInt(2 * 4) }
    val out_sel_pat = Array.fill(out_num) { rnd_gen.nextInt(le_num + carry_out_num) }

    poke(ic.io.cfg.sen, false)
    step(1)

    /* out sel */
    for (i <- 0 to out_num - 1) {
      val x = out_sel_pat(i)
      println(f"out sel[$i] = $x%d")
      for (j <- 0 to out_sel_width - 1) {
        poke(ic.io.cfg.sen, true)
        poke(ic.io.cfg.sin, (out_sel_pat(i) >> j) & 1)
        step(1)
      }
    }
    
    /* in sel */
    for (i <- 0 to le_num * lutx - 1) {
      val x = in_sel_pat(i)
      println(f" in sel[$i] = $x%d")
      for (j <- 0 to in_sel_width - 1) {
        poke(ic.io.cfg.sen, true)
        poke(ic.io.cfg.sin, (in_sel_pat(i) >> j) & 1)
        step(1)
      }
    }

    poke(ic.io.cfg.sen, false)
    step(1)

    step(1)
    step(1)
    for (m <- 0 to 0) {
      /* test input and verify output for a given pattern */
      val ins_pat = Array.fill(2 * 4) { rnd_gen.nextInt(2) }
      val plb_io_logics_n_carry_out_pat = Array.fill(le_num + carry_out_num) { rnd_gen.nextInt(2) }

      /* logic output from PLB */
      for (i <- 0 to le_num - 1) {
        val x = plb_io_logics_n_carry_out_pat(i)
        println(f" out [$i] = $x%d")
        poke(ic.io.plbio.logics(i).out, plb_io_logics_n_carry_out_pat(i))
      }

      poke(ic.io.plbio.carry.out, plb_io_logics_n_carry_out_pat(le_num))
      
      /* input from outside */
      for (i <- 0 to 2 * 4 - 1) {
        val x = ins_pat(i)
        println(f"  in [$i] = $x%d")
        poke(ic.io.ins(i), ins_pat(i))
      }

      step(1)

      /* check */
      for (i <- 0 to out_num - 1) {
        //TODO: if (out_sel_pat(i) <= 
      }
      
      /* in sel */
      for (i <- 0 to le_num - 1) {
        var exp = 0
        for (j <- 0 to lutx - 1) {
          exp = exp | (ins_pat(in_sel_pat(j + i * lutx)) << j)
          val x = (ins_pat(in_sel_pat(j + i * lutx)))
          println(f"test exp [$i][$j] = $x%d")
        }
        expect(ic.io.plbio.logics(i).in, exp)
      }

      //TODO: expect(ic.io.plbio.carry.in, 
    }
    step(1)
    step(1)
  }

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

package re._1r.circuit
import Component._
import Util._
import Numerics._
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.linear._

object Test {
  def test(cmpList: Array[Component], ndList: Array[Node]) {
      def x = cmpList.collect{case v: VoltageSrc => v}.map(v => {
    ndList.map(nd => {
      nd.reference = false
      nd.addVoltage = new Voltage(0,0,0,0)
    }) //TODO: merge on *not* v
    def cAlt = cmpList.filter(c => !(c.isInstanceOf[Source]) || c == v).map(c => if(c.neg == v.pos || c.pos == v.pos && v.neg.neg(cmpList).length == 1) c match {
      case z: Impeder => new SeriesImpeder(new Impeder(0, v.neg, v.pos, -1, "R"), z)
      case v1: VoltageSrc => new VoltageSrc(v1.voltage, v.neg, v1.pos, -1, "VSrc")
      case a => a
    } else c)
    merge(ndList, cAlt)
  })
  x.foreach(y => {
    ndList.map(nd => {
      nd.reference = false
      nd.addVoltage = new Voltage(0,0,0,0)
    })
    setReference(y._1, y._2)
    voltageImpl(y)
    y._1.foreach(n => println(n.toString + ": " + n.addVoltage(0) + ", " + n.reference + ", " + n.defined))
  })
  }
}
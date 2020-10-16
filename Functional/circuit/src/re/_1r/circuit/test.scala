package re._1r.circuit
import Component._
import Util._
import Numerics._
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.linear._

object Test extends App {
  Test1
}

object Test1 {
  val ndList = Vector(
    new Node(0),
    new Node(1),
    new Node(2),
    new Node(3)
  )
  val cmpList = Vector(
    new VoltageSrc(4, ndList(0), ndList(1), 0,"V1"),
    new Impeder(1.5e3, ndList(1), ndList(2), 0, "R1"),
    new Impeder(3.6e3, ndList(1), ndList(2), 1, "R2"),
    new Impeder(4.9e3, ndList(1), ndList(3), 2, "R3"),
    new Impeder(1.3e4, ndList(2), ndList(3), 3, "R4"),
    new Impeder(2.9e3, ndList(3), ndList(0), 4, "R5"),
    new VoltageSrc(1, ndList(1), ndList(2), 1, "V2")
  )
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
    //setReference(ndList, cAlt)
    merge(ndList, cAlt)
  })
  x.foreach(y => {
    ndList.map(nd => {
      nd.reference = false
      nd.addVoltage = new Voltage(0,0,0,0)
    })
    setReference(y._1, y._2)
    voltageImpl(y)
    println(y._2)
    y._1.foreach(n => println(n.toString + ": " + n.addVoltage(0) + ", " + n.reference + ", " + n.defined))
  })
}
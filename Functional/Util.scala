package re._1r.circuit

import Component._

object Util {
  def merge(nList: Vector[Node], cList: Vector[Component]): (Vector[Node], Vector[Component]) = {
    def mergeSeries(nList: Vector[Node], cList: Vector[Component]): (Vector[Node], Vector[Component]) = {
      def mergeFirstSeries(nList: Vector[Node], cList: Vector[Component]): (Vector[Node], Vector[Component]) = (nList.filter(nd => nd.pos(cList).length == 1 && nd.neg(cList).length == 1).find(nd => nd.pos(cList)(0).cmp == nd.neg(cList)(0).cmp) match {
        case a if a.nonEmpty => (a.get.pos(cList)(0), a.get.neg(cList)(0)) match {
          case b if b._1.isInstanceOf[Impeder] => new SeriesImpeder(b.asInstanceOf[(Impeder, Impeder)])
          case b if b._1.isInstanceOf[VoltageSrc] => new SeriesVoltageSrc(b.asInstanceOf[(VoltageSrc, VoltageSrc)])
          case _ => null
        }
        case _ => null
      }) match {
        case null => (nList, cList)
        case cmp => (nList.filterNot(_ == cmp.series._1.pos), cList.filterNot(c => c == cmp.series._1 || c == cmp.series._2) :+ cmp)
      }
      mergeFirstSeries(nList, cList) match {
        case a if a._1.length != nList.length => mergeSeries(a._1, a._2)
        case _ => (nList, cList)
      }
    }
    def mergeParallel(nList: Vector[Node], cList: Vector[Component]): Vector[Component] = {
      def mergeParallelImpeders(z: Vector[Impeder]): Impeder = z.tail match {
        case a if a.length == 0 => z.head
        case a if a.length == 1 => new ParallelImpeder(z.head, a(0))
        case a => mergeParallelImpeders(new ParallelImpeder(z.head, a(0)) +: a.tail)
      }
      if(nList.exists(_.pos(cList).count(_.cmp == 'V') > 1)) sys.error("Voltage sources cannot be parallel")
      cList.filter(_.cmp == 'V') ++ (cList.filter(_.cmp == 'Z').groupBy(z => (z.neg, z.pos))).map(a => mergeParallelImpeders(a._2.asInstanceOf[Vector[Impeder]])) //++ (cList.filter(_.cmp == 'I').groupBy(z => (z.neg, z.pos)).map(a => mergeParallelCurrentSources(a._2.asInstanceOf[Vector[CurrentSrc]])))
    }
    mergeSeries(nList, mergeParallel(nList, cList)) match {
      case ms if ms._2.length == cList.length && ms._1.length == nList.length => ms
      case ms => merge(ms._1, ms._2)
    }
  }
  def voltageImpl(mesh: (Vector[Node], Vector[Component])) = {
    mesh._1.foreach(nd => {
      def c = mesh._2(0)
      if(c.isInstanceOf[VoltageSrc]) nd match {
        case b if b == c.neg /*&& c.neg.voltage.parts.intersect(c.asInstanceOf[VoltageSrc].voltage.parts).length == 0*/ => {
          println(nd)
          println(c)
          c.pos.addVoltage += c.asInstanceOf[VoltageSrc].voltage
          c.pos.vInSource = c.neg
        }
        case _ => 
      }
    })
  }
  def setReference(a: Vector[Node], b: Vector[Component]) = a.find(_.neg(b).exists(_.isInstanceOf[VoltageSrc])).getOrElse(a.find(_.neg(b).exists(_.isInstanceOf[CurrentSrc])).getOrElse(a(0))).reference = true
  def impedanceImpl(lp: Vector[Mesh]) = {} 
}
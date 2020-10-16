package re._1r.circuit

import org.apache.commons.math3._
import complex._
import linear.Array2DRowFieldMatrix
import scala.math.sin
import Numerics._

object Component {
  trait VoltageComponent {
    def value: Double => Double
    def unary_- : VoltageComponent
    def apply(t: Double) = value(t)
    val amplitude: Double
    val omega: Double
    val phase: Double
    def *(d:Complex) = this match {
      case _: DcVoltageComponent => new DcVoltageComponent(amplitude * 2)
      case _: SineVoltageComponent => new SineVoltageComponent(amplitude, omega, phase)
    }
  }
  class SineVoltageComponent(val amplitude: Double, val omega: Double, val phase: Double) extends VoltageComponent {
    def this(v: Complex,  w: Double) = this(v.abs, w, v.getArgument)
    def value = (t: Double) => amplitude * sin(omega * t + phase)
    def unary_- = new SineVoltageComponent(-amplitude, omega, phase)
  }
  class DcVoltageComponent(v: Double) extends VoltageComponent {
    def value = (t: Double) => v
    val omega: Double = 0d
    val amplitude = 0d
    val phase = 0d
    def unary_- = new DcVoltageComponent(-v)
  }
  class Voltage(v: Vector[VoltageComponent]) {
    def this(a: Double, w: Double, ph: Double, dc: Double) = this((if (dc != 0) Vector(new DcVoltageComponent(dc)) else Vector()) ++ (if (a != 0) Vector(new SineVoltageComponent(a, w, ph)) else Vector()))
    def parts = v
    def +(x: Voltage) = new Voltage(parts ++ x.parts)
    def -(x: Voltage) = new Voltage(parts ++ x.parts.map(-_))
    def *(x: Complex) = new Voltage(parts.map(_ * x))
    def value = (t: Double) => v.foldLeft(0d)((acc, v) => acc + v(t))
    def omega = v.map(a => (a.amplitude, a.omega)).mean
    def apply(t: Double) = value(t)
  }
  class Node(val id: Int) {
    var reference: Boolean = false
    def <(nd: Node) = id < nd.id
    override def toString: String = id.toString
    def neg(cmpList: Vector[Component]) = cmpList.filter(_.neg == this)
    def pos(cmpList: Vector[Component]) = cmpList.filter(_.pos == this)
    def connections(cmpList: Vector[Component]) = neg(cmpList) ++ pos(cmpList)
    var addVoltage = new Voltage(0,0,0,0)
    def voltage: Voltage = if (vInSource != null) vInSource.voltage + addVoltage else addVoltage
    var vInSource: Node = null
    def defined: Boolean = (reference) || (vInSource != null && vInSource.defined)
  }
  trait Component {
    def value: Double => Any
    val name: String
    val id: Int
    def pos: Node
    def neg: Node
    val cmp: Char
    def onLoops(l: Vector[Mesh]): Vector[(Mesh, Int)] = l.zipWithIndex.filter(_._1.components.contains(this))
    override def toString: String = name
    var current: Double => Complex
  }
  trait SeriesComponent extends Component {
    def series: (Component, Component)
    override def pos: Node = series._2.pos
    override def neg: Node = series._1.neg
    override def toString = series.toString
  }
  trait ParallelComponent extends Component {
    def parallel: (Component, Component)
    override def pos: Node = parallel._1.pos
    override def neg: Node = parallel._1.neg
    override def toString = parallel.toString
  }
  trait Source
  class VoltageSrc (v: Voltage, n: Node, p: Node, val id: Int, val name: String) extends Component with Source {
    val cmp = 'V'
    def this (v: Voltage) = this(v, null, null, -1, "")
    def this (a: Double, w: Double, ph: Double, dc: Double, n: Node, p: Node, id: Int, name: String) = this(new Voltage(a, w, ph, dc), n, p, id, name)
    def this (dc: Double, n: Node, p: Node, id: Int, name: String) = this(new Voltage(0, 0, 0, dc), n, p, id, name)
    def voltage = v
    def pos = p
    def neg = n
    def value = (t: Double) => voltage(t)
    def apply(t: Double) = value(t)
  }
  class SeriesVoltageSrc (s: (VoltageSrc, VoltageSrc)) extends VoltageSrc (s._1.voltage + s._2.voltage) with SeriesComponent {
    def this(a: VoltageSrc, b: VoltageSrc) = this((a, b))
    def series: (VoltageSrc, VoltageSrc) = s
  }
  class CurrentSrc (val value: Double => Double, n: Node, p: Node, val id: Int, val name: String) extends Component with Source {
    def this(i: Double => Double, n: Node, p: Node) = this(i, n, p, -1, "")
    def apply(t: Double) = value(t)
    val cmp = 'I'
    def pos: Node = p
    def neg: Node = n
  }
  class ParallelCurrentSrc (pi: (CurrentSrc, CurrentSrc)) extends CurrentSrc((t: Double) => pi._1(t) + pi._2(t), null, null) with ParallelComponent {
    def this(a: CurrentSrc, b: CurrentSrc) = this((a, b))
    def parallel: (CurrentSrc, CurrentSrc) = pi
  }
  implicit class Impedance (val value: Double => Complex) {
    def +(z: Impedance): Double => Complex = (w: Double) => value(w) + z.value(w)
    def *(d: Double): Double => Complex = (w: Double) => value(w) * d
    def unary_- : Double => Complex = (w: Double) => -value(w)
  }
  class Impeder (val value: Double => Complex, n: Node, p: Node, val id: Int, val name: String) extends Component {
    def this(z: Double, n: Node, p: Node, id: Int, nm: String) = 
      this(nm(0) match {
        case 'R' => (omega: Double) => new Complex(z, 0d)
        case 'L' => (omega: Double) => new Complex(0d, omega * z)
        case 'C' => (omega: Double) => new Complex(0d, - 1 / (omega * z))
        case _ => (_: Double) => new Complex(0d,0d)
      }, n, p, id, nm)
    def this(z: Double => Complex, n: Node, p: Node) = this(z, n, p, -1, "")
    def pos = p
    def neg = n
    def apply(omega: Double) = value(omega)
    val cmp = 'Z'
    def current = if(pos.defined && neg.defined) {
      def v: Voltage = pos.voltage - neg.voltage
      (d: Double) => v.value(d) / value(v.omega)
    } else (d: Double) => new Complex(0d, 0d)
  }
  class ParallelImpeder (pz: (Impeder, Impeder)) extends Impeder ((omega: Double) => 1d / (1d / pz._1(omega) + 1d / pz._2(omega)), null, null) with ParallelComponent {
    def this(a: Impeder, b: Impeder) = this((a, b))
    def parallel: (Impeder, Impeder) = pz 
  }
  class SeriesImpeder (s: (Impeder, Impeder)) extends Impeder((omega: Double) => s._1(omega) + s._2(omega), null, null) with SeriesComponent {
    def this(a: Impeder, b: Impeder) = this((a, b))
    def series: (Impeder, Impeder) = s
  }
  implicit class Circuit(ndCm: (Vector[Node], Vector[Component])) {
    def this(nds: Vector[Node], cmps: Vector[Component]) = this((nds, cmps))
    def nodes: Vector[Node] = ndCm._1
    def components: Vector[Component] = ndCm._2
    override def toString: String = components.foldLeft("")((acc: String, v: Component) => acc + v.toString + "->").dropRight(2)
    def findLoops = {
      def findComponents(nd: Vector[Node], cmp: Vector[Component]) = {
        def findLoopFromComponent(c: Component, visited: Vector[Component], lastDir: Boolean): Vector[Vector[Component]] = if(lastDir) c.pos.neg(cmp).filterNot(a => visited.last == a).foldLeft(Vector[Vector[Component]]())((acc, v) => if (visited.contains(v)) Vector(visited :+ v) else acc ++ findLoopFromComponent(v, visited :+ v, true)) ++ c.pos.pos(cmp).filterNot(a => visited.last == a).foldLeft(Vector[Vector[Component]]())((acc, v) => if (visited.contains(v)) Vector(visited :+ v) else acc ++ findLoopFromComponent(v, visited :+ v, false)) else c.neg.neg(cmp).filterNot(a => visited.last == a).foldRight(Vector[Vector[Component]]())((v, acc) => if (visited.contains(v)) Vector(visited :+ v) else acc ++ findLoopFromComponent(v, visited :+ v, true)) ++ c.neg.pos(cmp).filterNot(a => visited.last == a).foldRight(Vector[Vector[Component]]())((v, acc) => if (visited.contains(v)) Vector(visited :+ v) else acc ++ findLoopFromComponent(v, visited :+ v, false))
        cmp.map(c => findLoopFromComponent(c, Vector(c), true).filter(vc =>  vc.length > 2 && vc.last == vc.head && ((vc.last.pos.neg(cmp).contains(vc.init.last) || vc.last.pos.pos(cmp).contains(vc.init.last)) && (vc.last.neg.pos(cmp).contains(vc.tail.head) || vc.last.neg.neg(cmp).contains(vc.tail.head)) || ((vc.last.neg.pos(cmp).contains(vc.init.last) || vc.last.neg.neg(cmp).contains(vc.init.last)) && (vc.last.pos.neg(cmp).contains(vc.tail.head) || vc.last.pos.pos(cmp).contains(vc.tail.head)))))).flatten.distinctBy(_.tail.sortBy(_.toString))
      }
      def findNodes(c: Vector[Component]): Vector[Node] = c.zipWithIndex.tail.foldLeft(Vector[Node]())((acc, v) => if(!(acc.contains(v._1.pos)) && v._1.pos.connections(c).contains(c(v._2 - 1))) acc :+ v._1.pos else acc :+ v._1.neg)
      findComponents(nodes, components).map(c => new Mesh(findNodes(c), c)).filter(l => l.nodes.tail.distinct.length == l.nodes.tail.length).sortBy(_.components.length).foldLeft(Vector[Mesh]())((acc, v) => if(v.components.diff(acc.foldLeft(Vector[Component]())((cp, lp) => cp ++ lp.components)).length == 0) acc else acc :+ v)
    }
  }
  class Mesh(nds: Vector[Node], cmps: Vector[Component]) extends Circuit(nds, cmps) {
    def current: Double => Double = {
      (t: Double) => 0d
    }
  }
  implicit class findCurrent(lp: Vector[Mesh]) {
    def matrix = {
      val a = Vector.tabulate(lp.length)(x => lp(x).components.collect{case v: VoltageSrc => v.voltage} match {
        case a if a.isEmpty => new Voltage(0, 0, 0, 0)
        case a => a.reduce((acc, v) => acc + v)
      })
      (Vector.tabulate(lp.length, lp.length)((x, y) => (lp(x).components.collect{case z: Impeder => z}.filter(_.onLoops(lp).exists(_._2 == y)).map(_.value).fold((_: Double) => new Complex(0,0))((acc, v) => acc + v) * (if (x == y) 1 else -1))(a(x).omega)).toMatrix, a)
      a.zipWithIndex.map(c => {
        (c._1.map(_(b(c._2).omega)), b(c._2))
      })
    }
  }
}
import breeze.linalg._
import breeze.interpolation._
import scalax.collection.{Graph,GraphTraversal}
import scalax.collection.GraphPredef._
import scalax.collection.edge._
import scalax.collection.GraphEdge.EdgeLike
import scala.io.Source._
import scala.collection.mutable.ArrayBuffer
import scala.math._

object circuits{
	implicit class getStuff(x:Array[String]){
		def getOrElse(index:Int, default:String):String = if(x.length > index) x(index) else default
	}
	def getComs:Sim = {
		val input = stdin.mkString
		val lines = input.split('\n').filter((line) => line(0) != '*')
		val command = lines.find((line) => line == ".op" || line.take(5) == ".tran").get
		val sim:Sim = {
			if(command == ".op"){
				new Sim(0d,0d,0d)
			}
			else{
				val params = command.drop(6).split(' ')
				val end = getValue(params.getOrElse(1,"10m"))
				val start = getValue(params.getOrElse(2,"0"))
				val ts = getValue(params.getOrElse(3,((end - start) / 1000d).toString))
				new Sim(start, end, ts)
			}
		}
		sim.nList += new Node(0)
		lines.mkString.split(' ').filter((p) => p(0) == 'N').distinct.sortBy((param) => param.tail.toInt).foreach((n) => {
			sim.nList += new Node(n.tail.toInt)
		})
		lines.filter((line) => line(0) == 'R').foreach((line) => {
			val params = line.split(' ')
			val uName = params(0)
			def nPos = if(params(1) == "0") sim.nList(0) else sim.nList(params(1).tail.toInt)
			def nNeg = if(params(2) == "0") sim.nList(0) else sim.nList(params(2).tail.toInt)
			val value = getValue(params(3))
			val id = sim.rList.length
			sim.rList += new Resistor(value, nNeg, nPos, id, 'R', uName)
		})
		lines.filter((line) => line(0) == 'L').foreach((line) => {
			val params = line.split(' ')
			val uName = params(0)
			def nPos = if(params(1) == "0") sim.nList(0) else sim.nList(params(1).tail.toInt)
			def nNeg = if(params(2) == "0") sim.nList(0) else sim.nList(params(2).tail.toInt)
			val value = getValue(params(3))
			val id = sim.vList.length
			sim.vList += new VSource(Seq(0d), id, nPos, nNeg, uName, 'L', true, sim.timestep)
		})
		lines.filter((line) => line(0) == 'V').foreach((line) => {
			val params = line.map((ch) => if(ch == '(' || ch == ')') ' ' else ch).split(' ')
			val uName = params(0)
			def nPos = if(params(1) == "0") sim.nList(0) else sim.nList(params(1).tail.toInt)
			def nNeg = if(params(2) == "0") sim.nList(0) else sim.nList(params(2).tail.toInt)
			val wv:Double = params(3) match{
				case "DC" => 0d
				case "SINE" => 1d
				case _ => -1d
			}
			val wavePs:Seq[Double] = Seq(getValue(params(4)), wv) ++ (params.drop(5).map(getValue(_)).toSeq)
			sim.vList += new VSource(wavePs, sim.vList.length, nPos, nNeg, uName, 'V', false, sim.timestep)
		})
		lines.filter((line) => line(0) == 'C').foreach((line) => {
			val params = line.split(' ')
			val uName = params(0)
			def nPos = if(params(1) == "0") sim.nList(0) else sim.nList(params(1).tail.toInt)
			def nNeg = if(params(2) == "0") sim.nList(0) else sim.nList(params(2).tail.toInt)
			val value = getValue(params(3))
			val id = sim.cList.length
			sim.cList += new CSource(Seq(0d,value), id, nPos, nNeg, uName, 'C', true, sim.timestep)
		})
		lines.filter((line) => line(0) == 'I').foreach((line) => {
			val params = line.map((ch) => if(ch == '(' || ch == ')') ' ' else ch).split(' ')

			val uName = params(0)
			def nPos = if(params(1) == "0") sim.nList(0) else sim.nList(params(1).tail.toInt)
			def nNeg = if(params(2) == "0") sim.nList(0) else sim.nList(params(2).tail.toInt)
			val wv:Double = params(3) match{
				case "DC" => 0d
				case "SINE" => 1d
				case _ => -1d
			}
			val wavePs:Seq[Double] = Seq(getValue(params(4)), wv) ++ (params.drop(5).map(getValue(_)).toSeq)
			sim.cList += new CSource(wavePs, sim.cList.length, nPos, nNeg, uName, 'C', false, sim.timestep)
		})
		sim
	}
	def getValue(num:String):Double = {
		num.last match{
			case 'y' => (num.init).toDouble * 1e-24d
			case 'z' => (num.init).toDouble * 1e-21d
			case 'a' => (num.init).toDouble * 1e-18d
			case 'f' => (num.init).toDouble * 1e-15d
			case 'p' => (num.init).toDouble * 1e-12d
			case 'n' => (num.init).toDouble * 1e-9d
			case 'u' => (num.init).toDouble * 1e-6d
			case 'μ' => (num.init).toDouble * 1e-6d
			case 'm' => (num.init).toDouble * 1e-3d
			case 'k' => (num.init).toDouble * 1e3d
			case 'g' => (num.dropRight(3)).toDouble * 1e6d
			case 'M' => (num.init).toDouble * 1e6d
			case 'G' => (num.init).toDouble * 1e9d
			case 'T' => (num.init).toDouble * 1e12d
			case 'P' => (num.init).toDouble * 1e15d
			case 'E' => (num.init).toDouble * 1e18d
			case 'Y' => (num.init).toDouble * 1e21d
			case 'Z' => (num.init).toDouble * 1e24d
			case _ => num.toDouble
		}
	}
	class Node(ID:Int){
		override def toString(): String = "Node" + ID.toString
		val id = ID
		var voltage = 0d
		var vPre1 = 0d
		var vPre2 = 0d
		var vPre3 = 0d
	}
	abstract class Component(cn:Char = 0.toChar, un:String = "", ID:Int = -1, p:Node = new Node(0), n:Node = new Node(0)){
		val cName:Char = cn
		val uName:String = un
		val id:Int = ID
		def neg:Node = n
		def pos:Node = p
	}
	case class Resistor(value:Double, n:Node, p:Node, ID:Int, CName:Char, UName:String) extends Component(CName, UName, ID, p, n){
		val resistance:Double = value
		def current = (pos.voltage - neg.voltage) / resistance
	}
	case class CSource(params:Seq[Double], ID:Int, p:Node, n:Node, un:String, cn:Char, dep:Boolean, ts:Double) extends Component(cn, un ,ID , p, n){
		def dcOffset:Double = params(0)
		def waveform:Double => Double = ((d:Double) => {
			cn match {
				case 'C' => {
					if(ts > 0){
						val y = DenseVector(pos.vPre3 - neg.vPre3,pos.vPre2 - neg.vPre2,pos.vPre1 - neg.vPre1,pos.voltage - neg.voltage, (pos.voltage - neg.voltage) * 3d - 2d * (pos.vPre1 - neg.vPre1))
						val x = DenseVector(0d, ts, ts * 2d, ts * 3d, ts * 5d)
						val f = CubicInterpolator(x,y)
						val g = f(ts * 4d)
						(g - (pos.voltage - neg.voltage)) * params(1) / ts
					}
					else 0d
				}
				case 'I' => params(1) match {
					case 0 => dcOffset
					case 1 => dcOffset + params(2) * sin(params(3) * 2 * Pi * d)
					case _ => 0d
				}
				case _ => 0d
			}
		})
	}
	case class VSource(params:Seq[Double], ID:Int, p:Node, n:Node, un:String, cn:Char, dep:Boolean, ts:Double) extends Component(cn, un ,ID , p, n){
		def dcOffset:Double = params(0)
		def waveform:Double => Double = ((d:Double) => {
			cn match {
				case 'L' => {
					if(ts > 0){
						val y = DenseVector(pos.vPre3 - neg.vPre3,pos.vPre2 - neg.vPre2,pos.vPre1 - neg.vPre1,pos.voltage - neg.voltage, (pos.voltage - neg.voltage) * 3d - 2d * (pos.vPre1 - neg.vPre1))
						val x = DenseVector(0d, ts, ts * 2d, ts * 3d, ts * 5d)
						val f = CubicInterpolator(x,y)
						f(ts * 4d)
					}
					else 0d
				}
				case 'V' => params(1) match {
					case 0 => dcOffset
					case 1 => dcOffset + params(2) * sin(params(3) * 2 * Pi * d)
					case _ => 0d
				}
				case _ => 0d
			}
		})
	}
	class Sim(s:Double = 0d, e:Double, ts:Double){
		val rList:ArrayBuffer[Resistor] = new ArrayBuffer[Resistor]
		val nList:ArrayBuffer[Node] = new ArrayBuffer[Node]
		val cList:ArrayBuffer[CSource] = new ArrayBuffer[CSource]
		val vList:ArrayBuffer[VSource] = new ArrayBuffer[VSource]
		val start:Double = s
		val end = e
		val timestep:Double = ts
		val steps:Int = ((e - s) / ts).toInt
		def this(s:Double, e:Double) = this(s, e, ((e-s)/1000d))
		def this(e:Double) = this(0d, e, e/1000d)
	}
}

object cirt extends App{
	import circuits._
	val s = getComs
	val edges = {
		s.rList.foldLeft(List[WkLkUnDiEdge[Node]]())((acc,r) => acc :+ WkLkUnDiEdge(r.neg,r.pos)(0d,r)) ++
		s.cList.foldLeft(List[WkLkUnDiEdge[Node]]())((acc,c) => if(c.cName == 'I') acc :+ WkLkDiEdge(c.neg,c.pos)(0d,c) else acc :+ WkLkUnDiEdge(c.neg,c.pos)(0d,c)) ++
		s.vList.foldLeft(List[WkLkUnDiEdge[Node]]())((acc,v) => if(v.cName == 'V') acc :+ WkLkDiEdge(v.neg,v.pos)(0d,v) else acc :+ WkLkUnDiEdge(v.neg,v.pos)(0d,v))
	}
	println(edges)
	val cgr = Graph.from(s.nList.toList,edges)
	println(cgr)
	println("Connected? " + cgr.isConnected)
	println("Cyclic? " + cgr.isCyclic)
/*	var x = WLkDiHyperEdge(s.nList(0),s.nList(1))(0d,s.rList(0))
	x.label match{
		case Resistor(value, n, p, id, cName, uName) => println(uName)
	}*/
}

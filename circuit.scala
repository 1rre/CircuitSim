import breeze.linalg._
import breeze.linalg.DenseMatrix.{vertcat,horzcat}
import breeze.interpolation._
import breeze.integrate._
import scala.collection.mutable.ArrayBuffer
import scala.io.Source._
import scala.math._

object resultsViewer extends App{
	import plotUtils._
	import CirClasses._
	val sim = getComs
	var output:String = ""
	output += "time"
	output += (
		sim.nList.tail.foldLeft("")((acc,nd) => acc + ",V(N" + nd.id.toString + ")") +
		sim.vList.foldLeft("")((acc,v) => acc + ",I(" + v.uName + ")") +
		sim.cList.foldLeft("")((acc,c) => acc + ",I(" + c.uName + ")") +
		sim.rList.filter((r) => r.id >= 0).foldLeft("")((acc,r) => acc + ",I(" + r.uName + ")") +
		'\n')
	val mSz = sim.nList.length + sim.vList.length + sim.cList.length + sim.rList.filter((r) => r.id >= 0).length
	var result:DenseMatrix[Double] = DenseMatrix.zeros[Double](0, mSz)
	val mb = MatB(sim)
	val ma = vertcat(horzcat(MatG(sim),mb),horzcat(mb.t,DenseMatrix.zeros[Double](sim.vList.size,sim.vList.size)))
	var mz = vertcat(MatI(sim, 0d),MatE(sim,result,0d))
	var mx = ma \ mz
	val invma = inv(ma)
	for(step <- 0 to sim.steps){
		val time:Double = step.toDouble * sim.timestep
		mz = vertcat(MatI(sim, time),MatE(sim, result, time))
		mx = ma \ mz
		sim.nList.foreach((nd) => {
			nd.vPre3 = nd.vPre2
			nd.vPre2 = nd.vPre1
			nd.vPre1 = nd.voltage
			nd.voltage = mx(nd.id - 1, 0)
		})
		sim.vList.foreach((v) => v.current = mx(v.id + sim.nList.length - 1, 0))
		result = vertcat(result, DenseMatrix.tabulate(1, mSz)((r,c) => {
			if(c == 0) time
			else if(c <  sim.nList.length) sim.nList(c).voltage
			else if(c < sim.nList.length + sim.vList.length) sim.vList(c - sim.nList.length).current
			else if(c < sim.nList.length + sim.vList.length + sim.cList.length) sim.cList(c - sim.nList.length - sim.vList.length).waveform(time)
			else sim.rList(c - sim.nList.length - sim.vList.length - sim.cList.length).current
		}))
	}
	val wsp = "[ ]+"
	output += result.toString(Int.MaxValue,Int.MaxValue).replaceAll(wsp, ",").split('\n').map(_.init + '\n').mkString
	println(output)
}

object CirClasses{
	implicit class getStuff(x:Array[String]){
		def getOrElse(index:Int, default:String):String = if(x.length > index) x(index) else default
	}
	implicit class addF(fn:Double => Double){
		def +(fn0:Double=>Double) = (d:Double) => fn(d) + fn0(d)
	}
	def tr(time:DenseVector[Double], points:DenseVector[Double]):Double = {
		val tm = time.toArray
		val pts = points.toArray
		if(tm.length>1){
			var last = (0d,0d)
			val fn:Double => Double = tm.zip(pts).foldLeft((d:Double) => 0d)((acc:Double => Double, v) => {
				val fn0 = ((d:Double) => if(d > last._1 && d <= v._1) (d - last._1) * v._2 / last._2 + last._2 else 0d)
				last = v
				acc + fn0
			})
			simpson(fn,tm(0), tm.last, time.size) + pts(0)
		}
		else 0d
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
			val id = sim.vList.length
			if(!sim.nList.exists((nd) => nd.id == -id)) sim.nList += new Node(-id)
			sim.vList += new VSource(Seq(0d,value), id, nPos, sim.nList.find((nd) => nd.id == -id).get, uName, 'C', true, sim.timestep) //
			val serR = 1d/(value)
			val parR = 1d/(value)
			sim.rList += new Resistor(serR, nNeg, sim.nList.find((nd) => nd.id == -id).get, -id, 'C', "R" + uName)
			sim.rList += new Resistor(parR, sim.nList.find((nd) => nd.id == -id).get, nPos, -id, 'C', "R" + uName)
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
	class Resistor(value:Double, n:Node, p:Node, ID:Int, CName:Char, UName:String) extends Component(CName, UName, ID, p, n){
		val resistance:Double = value
		def current = (pos.voltage - neg.voltage) / resistance
	}
	class CSource(params:Seq[Double], ID:Int, p:Node, n:Node, un:String, cn:Char, dep:Boolean, ts:Double) extends Component(cn, un ,ID , p, n){
		def dcOffset:Double = params(0)
		def waveform:Double => Double = ((d:Double) => {
			cn match {
				case 'I' => params(1) match {
					case 0 => dcOffset
					case 1 => dcOffset + params(2) * sin(params(3) * 2 * Pi * d)
					case _ => 0d
				}
				case _ => 0d
			}
		})
	}
	class VSource(params:Seq[Double], ID:Int, p:Node, n:Node, un:String, cn:Char, dep:Boolean, ts:Double) extends Component(cn, un ,ID , p, n){
		var current:Double = 0
		def dcOffset:Double = params(0)
		def waveform(results:DenseMatrix[Double], sim:Sim):Double => Double = ((d:Double) => {
			cn match {
				case 'L' => {
					if(ts > 0){
						val y = DenseVector(pos.vPre3 - neg.vPre3,pos.vPre2 - neg.vPre2,pos.vPre1 - neg.vPre1,pos.voltage - neg.voltage, (pos.voltage - neg.voltage) * 4d - 2d * (pos.vPre1 - neg.vPre1))
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
				case 'C' => {
					val time = results(::,0).toDenseVector
					val cap = results(::,sim.nList.length + ID).toDenseVector
					tr(time, cap) / params(1)
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
	def MatG(s:Sim):DenseMatrix[Double] = {
		DenseMatrix.tabulate(s.nList.length-1, s.nList.length-1){ case (i,j) => {
				if(i==j){
					s.rList.filter((r) => r.pos.id == s.nList(i+1).id || r.neg.id == s.nList(i+1).id).foldLeft(0d)((acc,r) => acc + 1d / r.resistance)
				}
				else{
					s.rList.filter((r) => (r.pos.id == s.nList(i+1).id && r.neg.id == s.nList(j+1).id || r.pos.id == s.nList(j+1).id && r.neg.id == s.nList(i+1).id)).foldLeft(0d)((acc,r) => acc - 1d / r.resistance)
				}
			}
		}
	}
	def MatB(s:Sim):DenseMatrix[Double] = {
		DenseMatrix.tabulate(s.nList.size - 1, s.vList.size){ case (i,j) => {
				if(s.vList(j).pos == s.nList(i+1)) 1d else if(s.vList(j).neg == s.nList(i+1)) -1d else 0d
			}
		}
	}
	def MatI(s:Sim, time:Double):DenseMatrix[Double] = {
		DenseMatrix.tabulate(s.nList.length - 1, 1){ case (i,j) => {
				s.cList.filter((c) => c.pos.id == i+1).foldLeft(0d)((acc,c) => acc - c.waveform(time)) + s.cList.filter((c) => c.neg.id == i+1).foldLeft(0d)((acc,c) => acc + c.waveform(time))
			}
		}
	}
	def MatE(s:Sim,result:DenseMatrix[Double],time:Double):DenseMatrix[Double] = {
		DenseMatrix.tabulate(s.vList.length, 1){ case (i,j) => {
				s.vList(i).waveform(result,s)(time)
			}
		}
	}
}

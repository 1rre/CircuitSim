import breeze.linalg._
import breeze.linalg.DenseMatrix.{vertcat,horzcat}
import breeze.interpolation._
import breeze.integrate._
import scala.collection.mutable.ArrayBuffer
import scala.io.Source._
import scala.math._
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.image.{Image,WritableImage}
import scalafx.scene._
import scalafx.collections._
import scalafx.scene.shape.Circle
import scalafx.scene.chart._
import scalafx.scene.text.{Text,Font}
import scalafx.scene.input.{MouseEvent,ScrollEvent,MouseDragEvent}
import scalafx.scene.layout._
import scalafx.scene.layout.Priority.{Always,Sometimes}
import scalafx.scene.chart.XYChart.{Series,Data}
import scalafx.scene.control.{Label,Tooltip,ListView,CheckBox,Button}
import scalafx.scene.control.cell.CheckBoxListCell
import scalafx.stage.{Stage,FileChooser,Window}
import scalafx.geometry.Pos.{CenterLeft,CenterRight,BottomLeft}
import scalafx.geometry.Insets
import scalafx.util.{Duration,StringConverter}
import scalafx.beans.property.{BooleanProperty,ObjectProperty}
import scalafx.embed.swing.SwingFXUtils.fromFXImage
import scalafx.scene.paint.Color.web

object plotUtils{
	val enableTT = BooleanProperty(false)
	def hn(x:Double,y:Double):Label = {
		val rtn = new Label
		rtn.visible() = false;
		rtn.setPrefSize(10d,10d)
		val tt = new Tooltip("(" + x.toString + "," + y.toString + ")")
		tt.showDelay = new Duration(0d)
		rtn.tooltip = tt
		rtn.style = "-fx-background-color: transparent;"
		enableTT.onChange{
			rtn.visible() = !rtn.visible()
		}
		rtn
	}
}


object resultsViewer{ //extends JFXApp
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
    stage = new PrimaryStage{
		maximized = true
		icons.add(new Image("file:project/lg.ico"))
        title = "Results Viewer"
        scene = new Scene{
			val in = output.split('\n')
			val lines = in.map(_.split(','))
			val sci = StringConverter[Number]((s:String) => s.toDouble, (d:Number) => {
				val split = d.asInstanceOf[Double].toString.split('E')
				if(split.length == 2){
					split(0).take(6) + Seq.fill[Char](6-split(0).take(6).length)('0').mkString + "E" + split(1)
				}
				else{
					split(0).take(6) + Seq.fill[Char](6-split(0).take(6).length)('0').mkString
				}
			})
			val cursorLoc = new Text(""){
				visible() = false
				alignmentInParent = BottomLeft
			}
			val xAxis  = NumberAxis("Time")
			xAxis.forceZeroInRange() = false
			xAxis.tickLabelFormatter() = sci
			xAxis.autoRanging() = true
			xAxis.upperBound.onChange{
				xAxis.tickUnit() = (xAxis.upperBound() - xAxis.lowerBound()) / 15d
			}
			xAxis.lowerBound.onChange{
				xAxis.tickUnit() = (xAxis.upperBound() - xAxis.lowerBound()) / 15d
			}
			val yAxis = NumberAxis("Voltage | Current")
			yAxis.forceZeroInRange() = false
			yAxis.tickLabelFormatter() = sci
			yAxis.autoRanging() = true
			yAxis.upperBound.onChange{
				yAxis.tickUnit() =(yAxis.upperBound() - yAxis.lowerBound()) / 10d
			}
			yAxis.lowerBound.onChange{
				yAxis.tickUnit() =(yAxis.upperBound() - yAxis.lowerBound()) / 10d
			}
			val xAxisAR = new CheckBox("Autorange Time Axis"){
				selected() = true
				padding() = Insets(5,10,5,10)
				selected.onChange{
					xAxis.autoRanging() = !xAxis.autoRanging()
				}
			}
			val yAxisAR = new CheckBox("Autorange Data Axis"){
				selected() = true
				padding() = Insets(5,10,5,10)
				selected.onChange{
					yAxis.autoRanging() = !yAxis.autoRanging()
				}
			}
			val xAxis0 = new CheckBox("Start Time Axis at 0"){
				selected() = false
				padding() = Insets(5,10,5,10)
				selected.onChange{
					xAxis.forceZeroInRange() = !xAxis.forceZeroInRange()
					if(xAxis.lowerBound() > 0){
						xAxis.lowerBound() = 0
					}
					else if(xAxis.upperBound() < 0){
						xAxis.upperBound() = 0
					}
				}
			}
			val yAxis0 = new CheckBox("Start Data Axis at 0"){
				selected() = false
				padding() = Insets(5,10,5,10)
				selected.onChange{
					yAxis.forceZeroInRange() = !yAxis.forceZeroInRange()
					if(yAxis.lowerBound() > 0){
						yAxis.lowerBound() = 0
					}
					else if(yAxis.upperBound() < 0){
						yAxis.upperBound() = 0
					}
				}
			}
			val xAxisLock = new CheckBox("Lock Time Axis"){
				selected() = false
				disable() = true
				padding() = Insets(5,10,5,10)
				selected.onChange{
					xAxis0.disable() = !xAxis0.disable()
					xAxisAR.disable() = !xAxisAR.disable()
				}
			}
			val yAxisLock = new CheckBox("Lock Data Axis"){
				selected() = false
				disable() = true
				padding() = Insets(5,10,5,10)
				selected.onChange{
					yAxis0.disable() = !yAxis0.disable()
					yAxisAR.disable() = !yAxisAR.disable()
				}
			}
			val enableTTs = new CheckBox("Enable Values On Hover"){
				selected() = false
				padding() = Insets(5,10,5,10)
				selected.onChange{
					enableTT() = !enableTT()
				}
			}
			xAxis.autoRanging.onChange{
				xAxisLock.disable() = xAxis.autoRanging()
			}
			yAxis.autoRanging.onChange{
				yAxisLock.disable() = yAxis.autoRanging()
			}
			//val sci = ObjectProperty(StringConverter[Double]((s:String) => s.toDouble, (d:Double) => d.toString))
			var graph = new LineChart(xAxis,yAxis) {
				title = "Circuit Details"
				hgrow = Always
				legendVisible() = false
				legendVisible.onChange{
					legendVisible() = false
				}
				onMouseEntered = (me:MouseEvent) => {
					if(me.x > (82d * width() / 1670d)  && me.x < (1659d * width() / 1670d) && me.y > (42d * height() / 1016d) && me.y < (956d * height() / 1016d)){
						cursorLoc.visible() = true
						val xLoc:Double = ((me.x - 82d * width() / 1670d) / (1575d * width() / 1670d) * (xAxis.upperBound - xAxis.lowerBound).toDouble + xAxis.lowerBound.toDouble)
						val yLoc:Double = (((956d * height() / 1016d) - me.y) / (912d * height() / 1016d) * (yAxis.upperBound - yAxis.lowerBound).toDouble + yAxis.lowerBound.toDouble)  //((height() - me.y + 43d) / 956d * (yAxis.upperBound - yAxis.lowerBound).toDouble + yAxis.lowerBound.toDouble)
						cursorLoc.text = "(" +  xLoc  + ", " + yLoc + ")"
					}
				}
				onMouseMoved = (me:MouseEvent) => {
					//1670, 1016
					if(!(me.x > (82d * width() / 1670d)  && me.x < (1659d * width() / 1670d) && me.y > (42d * height() / 1016d) && me.y < (956d * height() / 1016d))){
						cursorLoc.visible() = false
					}
					else{
						cursorLoc.visible() = true
						val xLoc:Double = ((me.x - 82d * width() / 1670d) / (1575d * width() / 1670d) * (xAxis.upperBound - xAxis.lowerBound).toDouble + xAxis.lowerBound.toDouble)
						val yLoc:Double = (((956d * height() / 1016d) - me.y) / (912d * height() / 1016d) * (yAxis.upperBound - yAxis.lowerBound).toDouble + yAxis.lowerBound.toDouble)  //((height() - me.y + 43d) / 956d * (yAxis.upperBound - yAxis.lowerBound).toDouble + yAxis.lowerBound.toDouble)
						cursorLoc.text = "(" +  xLoc  + ", " + yLoc + ")"
					}
				}
				onMouseExited = (me:MouseEvent) => {
					cursorLoc.visible() = false
				}
				onScroll = (me:ScrollEvent) =>{
					if(me.controlDown){ //Zoom
						if(!xAxisLock.selected() && !xAxisAR.selected()){
							val diff = xAxis.upperBound() - xAxis.lowerBound()
							if(xAxis0.selected()){
								if(xAxis.upperBound() >= 0){
									xAxis.upperBound() = xAxis.upperBound() + me.deltaY * diff / height()
								}
								if(xAxis.lowerBound() <= 0){
									xAxis.lowerBound() = xAxis.lowerBound() - me.deltaY * diff / height()
								}
							}
							else{
								xAxis.lowerBound() = xAxis.lowerBound() + me.deltaY * diff / height()
								xAxis.upperBound() = xAxis.upperBound() - me.deltaY * diff / height()
							}
						}
						if(!yAxisLock.selected() && !yAxisAR.selected()){
							val diff = yAxis.upperBound() - yAxis.lowerBound()
							if(yAxis0.selected()){
								if(yAxis.upperBound() >= 0){
									yAxis.upperBound() = yAxis.upperBound() + me.deltaY * diff / height()
								}
								if(yAxis.lowerBound() <= 0){
									yAxis.lowerBound() = yAxis.lowerBound() - me.deltaY * diff / height()
								}
							}
							else{
								yAxis.lowerBound() = yAxis.lowerBound() + me.deltaY * diff / height()
								yAxis.upperBound() = yAxis.upperBound() - me.deltaY * diff / height()
							}
						}
					}
					else if(me.shiftDown && !xAxisLock.selected() && !xAxisAR.selected()){ //Move in X
						val diff = xAxis.upperBound() - xAxis.lowerBound()
						if(xAxis0.selected()){
							if(xAxis.upperBound() >= 0){
								xAxis.upperBound() = xAxis.upperBound() - me.deltaX * diff / width()
							}
							if(xAxis.lowerBound() <= 0){
								xAxis.lowerBound() = xAxis.lowerBound() - me.deltaX * diff / width()
							}
						}
						else{
							xAxis.lowerBound() = xAxis.lowerBound() - me.deltaX * diff / width()
							xAxis.upperBound() = xAxis.upperBound() - me.deltaX * diff / width()
						}
					}
					else if(!yAxisLock.selected() && !yAxisAR.selected()){ //Move in Y
						val diff = yAxis.upperBound() - yAxis.lowerBound()
						if(yAxis0.selected()){
							if(yAxis.upperBound() >= 0){
								yAxis.upperBound() = yAxis.upperBound() + me.deltaY * diff / height()
							}
							if(yAxis.lowerBound() <= 0){
								yAxis.lowerBound() = yAxis.lowerBound() + me.deltaY * diff / height()
							}
						}
						else{
							yAxis.lowerBound() = yAxis.lowerBound() + me.deltaY * diff / height()
							yAxis.upperBound() = yAxis.upperBound() + me.deltaY * diff / height()
						}
					}
					me.consume()
				}
				var origin = (0d,0d,0d,0d,0d,0d)
				onMousePressed = (me:MouseEvent) => {
					origin = (me.sceneX,xAxis.lowerBound(),xAxis.upperBound(),me.sceneY,yAxis.lowerBound(),yAxis.upperBound())
				}
				onMouseReleased = (me:MouseEvent) => {
					origin = (0d,0d,0d,0d,0d,0d)
				}
				onMouseDragged = (me:MouseEvent) => {
					if(!xAxisLock.selected() && !xAxisAR.selected()){
						val diff = origin._3 - origin._2
						if(xAxis0.selected()){
							if(xAxis.upperBound() >= 0){
								xAxis.upperBound() = origin._3 + (origin._1 - me.sceneX) * diff / width()
							}
							if(xAxis.lowerBound() <= 0){
								xAxis.lowerBound() = origin._2 + (origin._1 - me.sceneX) * diff / width()
							}
						}
						else{
							xAxis.lowerBound() = origin._2 + (origin._1 - me.sceneX) * diff / width()
							xAxis.upperBound() = origin._3 + (origin._1 - me.sceneX) * diff / width()
						}
					}
					if(!yAxisLock.selected() && !yAxisAR.selected()){
						val diff = origin._6 - origin._5
						if(yAxis0.selected()){
							if(yAxis.upperBound() >= 0){
								yAxis.upperBound() = origin._6 - (origin._4 - me.y) * diff / height()
							}
							if(yAxis.lowerBound() <= 0){
								yAxis.lowerBound() = origin._5 - (origin._4 - me.y) * diff / height()
							}
						}
						else{
							yAxis.lowerBound() = origin._5 - (origin._4 - me.y) * diff / height()
							yAxis.upperBound() = origin._6 - (origin._4 - me.y) * diff / height()
						}
					}
					me.consume()
				}
			}

			val series = lines(0).zipWithIndex.tail.foldLeft(Seq[(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)]())((acc,sName) => {
				val srs = XYChart.Series.sfxXYChartSeries2jfx(new XYChart.Series[Number, Number]{
					name = sName._1
					data() ++= lines.tail.map(line => {
						val rtn = XYChart.Data[Number, Number](line(0).toDouble, line(sName._2).toDouble)
						rtn.setNode(hn(line(0).toDouble, line(sName._2).toDouble))
						rtn
					})
				})
				val bChange = BooleanProperty(false)
				bChange.onChange {
					if(bChange()){
						graph.data() = graph.data() ++= Seq(srs)
					}
					else{
						graph.data() = graph.data().filter(it => it != srs)
					}
				}
				acc :+ (srs,bChange)
			}).map(ta => (XYChart.Series.sfxXYChartSeries2jfx(ta._1), ta._2))
			val sc = StringConverter[(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)]((s:String) => series.find((ser) => ser._1.name() == s).get, {(v:(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)) => v._1.name()})
			val lv = new ListView[(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)](series){
				def lb = new CheckBoxListCell((item:(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)) => item._2,sc)
				cellFactory = ((lv) => lb)
				vgrow = Sometimes
			}
			val txt = new Text("Selected Waveforms:"){
				vgrow = Always
				hgrow = Always
				style = "-fx-font-size:18;"
			}
			val jpg = new Button("Export to JPG"){
				hgrow = Always
				alignment = CenterLeft
				onMousePressed = (me:MouseEvent) => {
					val fc = new FileChooser{
						title = "Save as JPG"
						extensionFilters ++= Seq(new FileChooser.ExtensionFilter("JPG Files", "*.jpg"))
					}
					var file = fc.showSaveDialog(stage)
					if(file!=null){
						val ss = graph.snapshot(new SnapshotParameters, null)
						val oImg = fromFXImage(ss,null)
						if(file.toString.dropRight(4) != ".jpg"){
							file = new java.io.File((file.toString :+ ".jpg").mkString)
						}
						javax.imageio.ImageIO.write(oImg, "JPEG", file)
					}
				}
			}
			val png = new Button("Export to PNG"){
				hgrow = Always
				alignment = CenterRight
				onMousePressed = (me:MouseEvent) => {
					val fc = new FileChooser{
						title = "Save as PNG"
						extensionFilters ++= Seq(new FileChooser.ExtensionFilter("PNG Files", "*.PNG"))
					}
					var file = fc.showSaveDialog(stage)
					if(file!=null){
						val ss = graph.snapshot(new SnapshotParameters, null)
						val oImg = fromFXImage(ss,null)
						if(file.toString.dropRight(4) != ".png"){
							file = new java.io.File((file.toString :+ ".png").mkString)
						}
						javax.imageio.ImageIO.write(oImg, "PNG", file)
					}
				}
			}
			val gp:GridPane = new GridPane(){
				border() = new Border(new BorderStroke(web("0x000000"),BorderStrokeStyle.Solid,CornerRadii.Empty,BorderWidths.Default))
			}
			gp.add(xAxisAR,0,0)
			gp.add(yAxisAR,0,1)
			gp.add(xAxis0,0,2)
			gp.add(yAxis0,0,3)
			gp.add(xAxisLock,0,4)
			gp.add(yAxisLock,0,5)
			gp.add(enableTTs,0,6)
			val hb = new HBox(jpg,png)
			val vb = new VBox(gp,hb,txt,lv)
			vb.width.onChange{
				hb.minWidth() = vb.width()
				hb.maxWidth() =  vb.width()
				txt.minWidth(vb.width())
				png.prefWidth() = vb.width()/2.25d
				jpg.prefWidth() = vb.width()/2.25d
				hb.spacing() = vb.width()/10
				hb.padding() = Insets(vb.width()/180,0,vb.width()/180,0)
			}
			val sp = new StackPane{
				hgrow = Always
			}
			sp.children = Seq(graph,cursorLoc)
			val box = new HBox(vb,sp)
            root = box
        }
    }
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

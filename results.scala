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
import scala.io.Source._
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

object resultsViewer extends JFXApp{
	import plotUtils._
    stage = new PrimaryStage{
		maximized = true
		icons.add(new Image("file:project/lg.ico"))
        title = "Results Viewer"
        scene = new Scene{
			val in = stdin.mkString.split('\n')
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
			val yAxis = NumberAxis("Voltage | Current")
			yAxis.forceZeroInRange() = false
			yAxis.tickLabelFormatter() = sci
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

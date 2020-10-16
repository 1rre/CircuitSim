import scalafx._, Includes._, application.JFXApp, JFXApp.PrimaryStage, scene._, image.{Image,WritableImage}, collections._, shape.Circle, chart._, text.{Text,Font,TextAlignment}, input.{MouseEvent,ScrollEvent,MouseDragEvent}, layout._ , Priority.{Always,Sometimes}, XYChart.{Series,Data}, control.{Label,Tooltip,ListView,CheckBox,Button}, control.cell.CheckBoxListCell, stage.{Stage,FileChooser,Window}, geometry.Pos.{CenterLeft,CenterRight,Center,BottomLeft}, geometry.Insets, scalafx.util.{Duration,StringConverter}, scala.io.Source._, beans.property.{BooleanProperty,ObjectProperty}, embed.swing.SwingFXUtils.fromFXImage, paint.Color.web
object plotUtils {
	val enableTT = BooleanProperty(false)
	def hn(x:Double,y:Double):Label = {
		val rtn = new Label
		rtn.visible() = false;
		rtn.setPrefSize(10d,10d)
		val tt = new Tooltip("(" + x.toString + "," + y.toString + ")")
		tt.showDelay = new Duration(0d)
		rtn.tooltip = tt
		rtn.style = "-fx-background-color: transparent;"
		enableTT.onChange {	rtn.visible() = !rtn.visible() }
		rtn
	}
}
object resultsViewer extends JFXApp {
	import plotUtils._
	stage = new PrimaryStage {
		maximized = true
		title = "Results Viewer"
		scene = new Scene{
			val in = stdin.mkString.split('\n')
			val lines = in.map(_.split(','))
			val sci = StringConverter[Number]((s:String) => s.toDouble, (d:Number) => {
				val split = d.asInstanceOf[Double].toString.split('E')
				if(split.length == 2) split(0).take(6).padTo(6, '0') + "E" + split(1)
				else split(0).take(6) + Seq.fill[Char](6-split(0).take(6).length)('0').mkString
			})
			val cursorLoc = new Text("") {
				visible() = false
				alignmentInParent = BottomLeft
			}
			val xAxis  = NumberAxis("Time")
			xAxis.forceZeroInRange() = false
			xAxis.tickLabelFormatter() = sci
			xAxis.upperBound.onChange { xAxis.tickUnit() = (xAxis.upperBound() - xAxis.lowerBound()) / 15d }
			xAxis.lowerBound.onChange { xAxis.tickUnit() = (xAxis.upperBound() - xAxis.lowerBound()) / 15d }
			val yAxis = NumberAxis("Voltage | Current")
			yAxis.forceZeroInRange() = false
			yAxis.tickLabelFormatter() = sci
			yAxis.upperBound.onChange { yAxis.tickUnit() =(yAxis.upperBound() - yAxis.lowerBound()) / 10d }
			yAxis.lowerBound.onChange { yAxis.tickUnit() =(yAxis.upperBound() - yAxis.lowerBound()) / 10d }
			val xAxisAR = new CheckBox("Autorange Time Axis"){
				selected() = true
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					xAxis.autoRanging() = !xAxis.autoRanging()
				}
			}
			val yAxisAR = new CheckBox("Autorange Data Axis"){
				selected() = true
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange { yAxis.autoRanging() = !yAxis.autoRanging() }
			}
			val xAxis0 = new CheckBox("Force 0 on Time Axis"){
				selected() = false
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					xAxis.forceZeroInRange() = !xAxis.forceZeroInRange()
					if(xAxis.lowerBound() > 0d) xAxis.lowerBound() = 0d
					else if(xAxis.upperBound() < 0d) xAxis.upperBound() = 0d
				}
			}
			val yAxis0 = new CheckBox("Force 0 on Data Axis"){
				selected() = false
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					yAxis.forceZeroInRange() = !yAxis.forceZeroInRange()
					if(yAxis.lowerBound() > 0d) { yAxis.lowerBound() = 0d }
					else if(yAxis.upperBound() < 0d) { yAxis.upperBound() = 0d }
				}
			}
			val xAxisLock = new CheckBox("Lock Time Axis"){
				selected() = false
				disable() = true
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					xAxis0.disable() = !xAxis0.disable()
					xAxisAR.disable() = !xAxisAR.disable()
				}
			}
			val yAxisLock = new CheckBox("Lock Data Axis"){
				selected() = false
				disable() = true
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					yAxis0.disable() = !yAxis0.disable()
					yAxisAR.disable() = !yAxisAR.disable()
				}
			}
			val enableTTs = new CheckBox("Enable Values On Hover"){
				selected() = false
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange { enableTT() = !enableTT() }
			}
			xAxis.autoRanging.onChange{
				xAxisLock.disable() = xAxis.autoRanging()
			}
			yAxis.autoRanging.onChange{
				yAxisLock.disable() = yAxis.autoRanging()
			}
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
						val yLoc:Double = (((956d * height() / 1016d) - me.y) / (912d * height() / 1016d) * (yAxis.upperBound - yAxis.lowerBound).toDouble + yAxis.lowerBound.toDouble)
						cursorLoc.text = "(" +  xLoc  + ", " + yLoc + ")"
					}
				}
				onMouseMoved = (me:MouseEvent) => {
					if(!(me.x > (82d * width() / 1670d)  && me.x < (1659d * width() / 1670d) && me.y > (42d * height() / 1016d) && me.y < (956d * height() / 1016d))){
						cursorLoc.visible() = false
					}
					else{
						cursorLoc.visible() = true
						val xLoc:Double = ((me.x - 82d * width() / 1670d) / (1575d * width() / 1670d) * (xAxis.upperBound - xAxis.lowerBound).toDouble + xAxis.lowerBound.toDouble)
						val yLoc:Double = (((956d * height() / 1016d) - me.y) / (912d * height() / 1016d) * (yAxis.upperBound - yAxis.lowerBound).toDouble + yAxis.lowerBound.toDouble) 
						cursorLoc.text = "(" +  xLoc  + ", " + yLoc + ")"
					}
				}
				onMouseExited = (me:MouseEvent) => {
					cursorLoc.visible() = false
				}

				//SCROLL FUNCTIONALITY ON GRAPH
				onScroll = (me:ScrollEvent) =>{
					if(me.controlDown){
						if(!xAxisLock.selected() && !xAxisAR.selected()){
							val diff = if (xAxis.upperBound() - xAxis.lowerBound() > 0) xAxis.upperBound() - xAxis.lowerBound() else 1e-100d
							if(xAxis0.selected()){
								if(xAxis.upperBound() > 0d){
									xAxis.upperBound() = xAxis.upperBound() + me.deltaY * diff / height()
								}
								if(xAxis.lowerBound() < 0d){
									xAxis.lowerBound() = xAxis.lowerBound() - me.deltaY * diff / height()
								}
							}
							else{
								xAxis.lowerBound() = xAxis.lowerBound() + me.deltaY * diff / height()
								xAxis.upperBound() = xAxis.upperBound() - me.deltaY * diff / height()
							}
						}
						if(!yAxisLock.selected() && !yAxisAR.selected()){
							val diff = if(yAxis.upperBound() - yAxis.lowerBound() > 0) yAxis.upperBound() - yAxis.lowerBound() else 1e-100d
							if(yAxis0.selected()){
								if(yAxis.upperBound() >= 0d){
									yAxis.upperBound() = yAxis.upperBound() + me.deltaY * diff / height()
								}
								if(yAxis.lowerBound() <= 0d){
									yAxis.lowerBound() = yAxis.lowerBound() - me.deltaY * diff / height()
								}
							}
							else{
								yAxis.lowerBound() = yAxis.lowerBound() + me.deltaY * diff / height()
								yAxis.upperBound() = yAxis.upperBound() - me.deltaY * diff / height()
							}
						}
					}
					else if(me.shiftDown && !xAxisLock.selected() && !xAxisAR.selected()){
						val diff = if (xAxis.upperBound() - xAxis.lowerBound() > 0) xAxis.upperBound() - xAxis.lowerBound() else 1e-100d
						if(xAxis0.selected()){
							if(xAxis.upperBound() > 0d){
								xAxis.upperBound() = xAxis.upperBound() - me.deltaX * diff / width()
							}
							if(xAxis.lowerBound() < 0d){
								xAxis.lowerBound() = xAxis.lowerBound() - me.deltaX * diff / width()
							}
						}
						else{
							xAxis.lowerBound() = xAxis.lowerBound() - me.deltaX * diff / width()
							xAxis.upperBound() = xAxis.upperBound() - me.deltaX * diff / width()
						}
					}
					else if(!yAxisLock.selected() && !yAxisAR.selected()){
						val diff = if(yAxis.upperBound() - yAxis.lowerBound() > 0) yAxis.upperBound() - yAxis.lowerBound() else 1e-100d
						if(yAxis0.selected()){
							if(yAxis.upperBound() >= 0d){
								yAxis.upperBound() = yAxis.upperBound() + me.deltaY * diff / height()
							}
							if(yAxis.lowerBound() <= 0d){
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

				//DRAG FUNCTIONALITY ON GRAPH
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
							if(xAxis.upperBound() > 0d){
								xAxis.upperBound() = origin._3 + (origin._1 - me.sceneX) * diff / width()
							}
							if(xAxis.lowerBound() < 0d){
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
							if(yAxis.upperBound() >= 0d){
								yAxis.upperBound() = origin._6 - (origin._4 - me.y) * diff / height()
							}
							if(yAxis.lowerBound() <= 0d){
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

			//SETUP THE DATA SERIES. We need to use a javafx series instead of a scalafx series (scalafx wraps javafx however some parts aren't 100% complete including the "checkboxlistcell" we are using next. This means we must use the javafx implementation) & we package it in a tuple with a boolean property (explained later)
			val series = lines.head.zipWithIndex.tail.foldLeft(Seq[(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)]())((acc,sName) => {
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
			})
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
			val outCsv = new Button("Export to\nCSV"){
				hgrow = Always
				textAlignment = TextAlignment.Center
				alignment = Center
				onMouseClicked = (me:MouseEvent) => {
					val fc = new FileChooser{
						title = "Save Dataset as CSV"
						extensionFilters += new FileChooser.ExtensionFilter("CSV Files", "*.csv")
					}
					val file = fc.showSaveDialog(stage).toString
					import java.io.FileWriter
					val writer = {
						if(file.takeRight(4) != ".csv" && file != "") new FileWriter(file + ".csv")
						else if(file != "") new FileWriter(file)
						else{
							import util.Properties.{userDir, osName}
							val separator = if (osName.take(3) == "Win") '\\' else '/'
							new FileWriter(userDir + separator + "out.csv")
						}
					}
					writer.write(in.mkString("\n"))
					writer.close
				}
			}
			val jpg = new Button("Export to\nJPG"){
				hgrow = Always
				textAlignment = TextAlignment.Center
				alignment = Center
				onMouseClicked = (me:MouseEvent) => {
					val fc = new FileChooser{
						title = "Save as JPG"
						extensionFilters += new FileChooser.ExtensionFilter("JPG Files", "*.jpg")
					}
					var file = fc.showSaveDialog(stage)
					if(file!=null){
						val ss = graph.snapshot(new SnapshotParameters, null)
						val oImg = fromFXImage(ss,null)
						if(file.toString.takeRight(4) != ".jpg"){
							file = new java.io.File(file.toString + ".jpg")
						}
						javax.imageio.ImageIO.write(oImg, "JPEG", file)
					}
				}
			}
			val png = new Button("Export to\nPNG"){
				hgrow = Always
				alignment = Center
				textAlignment = TextAlignment.Center
				onMousePressed = (me:MouseEvent) => {
					val fc = new FileChooser{
						title = "Save as PNG"
						extensionFilters += new FileChooser.ExtensionFilter("PNG Files", "*.PNG")
					}
					var file = fc.showSaveDialog(stage)
					if(file!=null){
						val ss = graph.snapshot(new SnapshotParameters, null)
						val oImg = fromFXImage(ss,null)
						if(file.toString.takeRight(4) != ".png"){
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
			val hb = new HBox(jpg,outCsv,png)
			val vb = new VBox(gp,hb,txt,lv)
			vb.width.onChange{
				hb.minWidth() = vb.width()
				hb.maxWidth() = vb.width()
				txt.minWidth(vb.width())
				png.prefWidth() = vb.width() / 3.1d
				jpg.prefWidth() = vb.width() / 3.1d
				outCsv.prefWidth = vb.width() / 3.1d
				hb.spacing() = vb.width()/60d
				hb.padding() = Insets(vb.width()/180d,0d,vb.width()/180d,0d)
			}
			val sp = new StackPane{
				hgrow = Always
				children = Seq(graph,cursorLoc)
			}
			val box = new HBox(vb,sp)
			root = box
		}
	}
}

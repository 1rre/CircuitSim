import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.image.{Image,WritableImage}
import scalafx.scene._
import scalafx.collections._
import scalafx.scene.chart._
import scalafx.scene.text.{Text,Font}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{VBox,HBox}
import scalafx.scene.layout.Priority.{Always,Sometimes}
import scalafx.scene.chart.XYChart.{Series,Data}
import scalafx.scene.control.{Label,Tooltip,ListView,CheckBox,Button}
import scalafx.scene.control.cell.CheckBoxListCell
import scalafx.stage.{Stage,FileChooser,Window}
import scalafx.geometry.Pos.{CenterLeft,CenterRight}
import scalafx.geometry.Insets
import scalafx.util.{Duration,StringConverter}
import scala.io.Source._
import scalafx.beans.property.BooleanProperty
import scalafx.embed.swing.SwingFXUtils.fromFXImage

object plotUtils{
	def hn(x:Double,y:Double):Label = {
		val rtn = new Label
		rtn.setPrefSize(10,10)
		val tt = new Tooltip("(" + x.toString + "," + y.toString + ")")
		tt.showDelay = new Duration(0d)
		rtn.tooltip = tt
		rtn.style = "-fx-background-color: transparent;"
		rtn
	}
}

object resultsViewer extends JFXApp{
	import plotUtils._
    stage = new PrimaryStage{
		icons.add(new Image("file:project/lg.ico"))
        title = "Results Viewer"
        scene = new Scene{
			val in = stdin.mkString.split('\n')
			val lines = in.map(_.split(','))
			var graph = new LineChart(NumberAxis("X Axis"), NumberAxis("Y Axis")) {
				title = "Circuit Details"
				hgrow = Always
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
					println("changed")
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
				def lb = new CheckBoxListCell((item:(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)) => item._2,sc){
				}
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
			val hb = new HBox(jpg,png)
			val vb = new VBox(txt,lv,hb)
			vb.width.onChange{
				hb.minWidth() = vb.width()
				hb.maxWidth() =  vb.width()
				txt.minWidth(vb.width())
				png.prefWidth() = vb.width()/2.25d
				jpg.prefWidth() = vb.width()/2.25d
				hb.spacing() = vb.width()/10
				hb.padding() = Insets(vb.width()/180,0,vb.width()/180,0)
			}
			val box = new HBox(vb,graph)
            root = box
        }
    }
}

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

object plotUtils{ //An object used to make tooltips showing the value appear when you hover over a data series
	val enableTT = BooleanProperty(false) //Are the tooltips enabled? Initally no.
	def hn(x:Double,y:Double):Label = { //An invisible box to appear at each point on the graph
		val rtn = new Label
		rtn.visible() = false; //As the tooltips are initially disabled, visible is set to false at the start
		rtn.setPrefSize(10d,10d) //Set the preferred size to 10px by 10px. 10d means 10 in double form.
		val tt = new Tooltip("(" + x.toString + "," + y.toString + ")") //Set the text of the tooltip to the x and y of the point on the graph
		tt.showDelay = new Duration(0d) //Set the tooltips to show 0 milliseconds after they are hovered over
		rtn.tooltip = tt //Attach the tooltip to our invisible box
		rtn.style = "-fx-background-color: transparent;" //Make the invisible box in
		enableTT.onChange{ //When the tooltips are set to be disabled
			rtn.visible() = !rtn.visible() //Hide the invisible box such that it can't be hovered over
		}
		rtn
	}
}

object resultsViewer extends JFXApp{
	import plotUtils._ //Import the invisible box
    stage = new PrimaryStage{ //Declare the main window of the program
		maximized = true //Maximise the window
		icons.add(new Image("file:project/lg.ico")) //Unacceptable
        title = "Results Viewer" //Set the window name to "Results Viewer"
        scene = new Scene{
			val in = stdin.mkString.split('\n') //Split all of std in into an array of lines (done after input is completed)
			val lines = in.map(_.split(',')) //Split each of the lines in the array into an array of data points
			val sci = StringConverter[Number]((s:String) => s.toDouble, (d:Number) => { //Create a version of "toString"/"toDouble" which converts to & from scientific notation
				val split = d.asInstanceOf[Double].toString.split('E') //Split the string at "E"
				if(split.length == 2){ //If there was already an E in the string (ie it was already in scientific notation)
					split(0).take(6).padTo(6, '0') + "E" + split(1) //Take the 1st 6 characters from the string if there are more than 6 or if there are fewer add '0's until there are 6, then add E followed by the exponent
				}
				else{ //If the string isn't already in scientific format
					split(0).take(6) + Seq.fill[Char](6-split(0).take(6).length)('0').mkString //Take the 1st 6 characters from the string if there are more than 6 or if there are fewer add '0's until there are 6
				}
			})
			val cursorLoc = new Text(""){ //Some text to appear when the user hovers over the graph & show their cursro location
				visible() = false //Initially invisible as the cursor will not be over the graph (and if it is it will generate a MouseEvent when the program loads)
				alignmentInParent = BottomLeft //Aligned in the bottom left. We will use a stackpane so that it will display over ("stacked on top of") the graph.
			}

			//CREATE THE AXES
			val xAxis  = NumberAxis("Time") //Create an x axis called "time"
			xAxis.forceZeroInRange() = false //Don't force the axis to have 0 in its rage
			xAxis.tickLabelFormatter() = sci //Format the axis tick points in the scientific way as described earlier
			xAxis.upperBound.onChange{
				xAxis.tickUnit() = (xAxis.upperBound() - xAxis.lowerBound()) / 15d
			}
			xAxis.lowerBound.onChange{
				xAxis.tickUnit() = (xAxis.upperBound() - xAxis.lowerBound()) / 15d
			}
			val yAxis = NumberAxis("Voltage | Current") //Create a y axis called "Voltage | Current"
			yAxis.forceZeroInRange() = false //Don't force the axis to have 0 in its rage
			yAxis.tickLabelFormatter() = sci //Format the axis tick points in the scientific way as described earlier
			yAxis.upperBound.onChange{
				yAxis.tickUnit() =(yAxis.upperBound() - yAxis.lowerBound()) / 10d
			}
			yAxis.lowerBound.onChange{
				yAxis.tickUnit() =(yAxis.upperBound() - yAxis.lowerBound()) / 10d
			}

			//CREATE THE CHECKBOXES
			val xAxisAR = new CheckBox("Autorange Time Axis"){ //Create a checkbox with a label saying "Autorange Time Axis"
				selected() = true //Set it to be selected when the program loads
				padding() = Insets(5d,10d,5d,10d) //Set a border of 5px on its sides and 10px
				selected.onChange{ //When the checkbox is seleted
					xAxis.autoRanging() = !xAxis.autoRanging() //Turn off the axis autoranging feature on the x axis
				}
			}
			//There are a lot so I won't repeat comments
			val yAxisAR = new CheckBox("Autorange Data Axis"){ //Same as above but for y axis
				selected() = true
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					yAxis.autoRanging() = !yAxis.autoRanging()
				}
			}
			val xAxis0 = new CheckBox("Force 0 on Time Axis"){
				selected() = false //Set the checkbox to be initially unselected
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					xAxis.forceZeroInRange() = !xAxis.forceZeroInRange() //If we were forcing 0 to be visible on the x axis, don't and vice versa
					if(xAxis.lowerBound() > 0d){ //If the minimum value on the x axis is above 0
						xAxis.lowerBound() = 0d //Set the minimum value on the x axis to 0
					}
					else if(xAxis.upperBound() < 0d){ //If the maximum value on the x axis is below 0
						xAxis.upperBound() = 0d //Set the maximum value on the x axis to 0
					}
				}
			}
			val yAxis0 = new CheckBox("Force 0 on Data Axis"){ //Same as above but for y axis
				selected() = false
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					yAxis.forceZeroInRange() = !yAxis.forceZeroInRange()
					if(yAxis.lowerBound() > 0d){
						yAxis.lowerBound() = 0d
					}
					else if(yAxis.upperBound() < 0d){
						yAxis.upperBound() = 0d
					}
				}
			}
			val xAxisLock = new CheckBox("Lock Time Axis"){
				selected() = false
				disable() = true //Set the checkbox to intially unselectable as we can't lock the axis when autoranging is available.
				padding() = Insets(5d,10d,5d,10d)
				selected.onChange{
					xAxis0.disable() = !xAxis0.disable() //If the "lock x axis to 0" checkbox is disabled, enable it & vice versa
					xAxisAR.disable() = !xAxisAR.disable() //If the "Autorange x axis" checkbox is disabled, enable it & vice versa
				}
			}
			val yAxisLock = new CheckBox("Lock Data Axis"){ //Same as above but for y axis
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
				selected.onChange{
					enableTT() = !enableTT() //Enable the tooltips & invisible boxes from plotUtils
				}
			}
			xAxis.autoRanging.onChange{ //When autoranging is enabled/disabled on the x axis
				xAxisLock.disable() = xAxis.autoRanging() //disable/enable the x axis lock accordingly
			}
			yAxis.autoRanging.onChange{ //When autoranging is enabled/disabled on the y axis
				yAxisLock.disable() = yAxis.autoRanging() //disable/enable the y axis lock accordingly
			}
			var graph = new LineChart(xAxis,yAxis) { //Make a new linechart
				title = "Circuit Details" //Create the title as "Circuit Details". Not entirely necessary but removing it would mess up my maths
				hgrow = Always //Always grow when the window is expanded horizontally (taking priority over other nodes)
				legendVisible() = false //Set the legend to not visible as having it would mess up the maths and it's obvious which is which because it appears when you check the box
				legendVisible.onChange{
					legendVisible() = false //Sometimes a legend appears when we do stuff like add data series. If one tries to, stop it.
				}
				onMouseEntered = (me:MouseEvent) => { //Whenever the mouse first enters the graph
					if(me.x > (82d * width() / 1670d)  && me.x < (1659d * width() / 1670d) && me.y > (42d * height() / 1016d) && me.y < (956d * height() / 1016d)){ //If the cursor is contained within the plot area (measured manually and calculated against the size of the graph as a whole)
						cursorLoc.visible() = true //Set the text showing the location of the cursor to visible
						val xLoc:Double = ((me.x - 82d * width() / 1670d) / (1575d * width() / 1670d) * (xAxis.upperBound - xAxis.lowerBound).toDouble + xAxis.lowerBound.toDouble) //measure the x location of the cursor and scale it according to the values on the x axis
						val yLoc:Double = (((956d * height() / 1016d) - me.y) / (912d * height() / 1016d) * (yAxis.upperBound - yAxis.lowerBound).toDouble + yAxis.lowerBound.toDouble) //measure the y location of the cursor and scale it according to the values on the y axis
						cursorLoc.text = "(" +  xLoc  + ", " + yLoc + ")" //Set the text to reflect this location.
					}
				}
				onMouseMoved = (me:MouseEvent) => { //Do basically the same thing if the mouse is moved within the graph, except for we add an if statement to set the text to invisible if the cursor leaves the plot area
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
				onMouseExited = (me:MouseEvent) => { //Because the cursor won't always leave the graph by the sides (alt tab, ctrl alt del etc.), also disable the text whenever the mouse leaves the graph area.
					cursorLoc.visible() = false
				}

				//SCROLL FUNCTIONALITY ON GRAPH
				onScroll = (me:ScrollEvent) =>{ //When the user scrolls their mouse on the graph area
					if(me.controlDown){ //Zoom if control is held down
						if(!xAxisLock.selected() && !xAxisAR.selected()){ //If the x axis is neither locked or set to autorange
							val diff = if (xAxis.upperBound() - xAxis.lowerBound() > 0) xAxis.upperBound() - xAxis.lowerBound() else 1e-100d //calculate the current difference between the max and min of the axis. If it's 0 set it to a very small value to prevent the axis getting "stuck" at 0 (can happen when autoranging is on and a dc waveform is viewed then autoranging turned off)
							if(xAxis0.selected()){ //If we are forcing 0 in range
								if(xAxis.upperBound() > 0d){ //If the upper bound is greater than 0
									xAxis.upperBound() = xAxis.upperBound() + me.deltaY * diff / height() //Add the "delta Y" (amount scrolled - I'm not sure why it's split into x & y when scrolling is unidirectional) to the maximum value
								}
								if(xAxis.lowerBound() < 0d){ //if the lower bound is less than 0
									xAxis.lowerBound() = xAxis.lowerBound() - me.deltaY * diff / height() //subtract the "delta Y" (amount scrolled - I'm not sure why it's split into x & y when scrolling is unidirectional) from the minimum value
								}
							}
							else{ //If we aren't forcing 0 in range, do the same as above but without if statements. This 3/1 split on if statements is preferable to using 2 if statements as this will be a more common situation.
								xAxis.lowerBound() = xAxis.lowerBound() + me.deltaY * diff / height()
								xAxis.upperBound() = xAxis.upperBound() - me.deltaY * diff / height()
							}
						}
						if(!yAxisLock.selected() && !yAxisAR.selected()){ //Same as above but for the y axis
							val diff = if(yAxis.upperBound() - yAxis.lowerBound() > 0) yAxis.upperBound() - yAxis.lowerBound() else 1e-100d //same as above but for y axis
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
					else if(me.shiftDown && !xAxisLock.selected() && !xAxisAR.selected()){ //Move in X if shift is held down and the x axis is not locked or set to autorange
						val diff = if (xAxis.upperBound() - xAxis.lowerBound() > 0) xAxis.upperBound() - xAxis.lowerBound() else 1e-100d
						if(xAxis0.selected()){
							if(xAxis.upperBound() > 0d){
								xAxis.upperBound() = xAxis.upperBound() - me.deltaX * diff / width() //If the upper bound is above 0, subtract the deltax from it. It's delta x this time for some reason
							}
							if(xAxis.lowerBound() < 0d){
								xAxis.lowerBound() = xAxis.lowerBound() - me.deltaX * diff / width() //If the lower bound is below 0, subtract the deltax from it
							}
						}
						else{ //If the x axis lock is not on do the above without the if statements.
							xAxis.lowerBound() = xAxis.lowerBound() - me.deltaX * diff / width()
							xAxis.upperBound() = xAxis.upperBound() - me.deltaX * diff / width()
						}
					}
					else if(!yAxisLock.selected() && !yAxisAR.selected()){ //If the shift and control keys are unpressed, scroll the y axis
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
					me.consume() //Delete the mouse event
				}

				//DRAG FUNCTIONALITY ON GRAPH
				var origin = (0d,0d,0d,0d,0d,0d) //Set the origin to a 6 part tuple of doubles
				onMousePressed = (me:MouseEvent) => { //When the mouse is pressed (and not instantly released) within the graph area
					origin = (me.sceneX,xAxis.lowerBound(),xAxis.upperBound(),me.sceneY,yAxis.lowerBound(),yAxis.upperBound()) //Set the origin to the location of the click & the current bounds of the graph
				}
				onMouseReleased = (me:MouseEvent) => { //When the mouse is released after being held down (not after a click which is a different MouseEvent)
					origin = (0d,0d,0d,0d,0d,0d) //Set the origin to all 0s. Not entirely necessary but it means that panning won't happen due to bugs
				}
				onMouseDragged = (me:MouseEvent) => { //When the mouse is moved while clicked
					if(!xAxisLock.selected() && !xAxisAR.selected()){ //If the x axis is neither locked or set to autorange
						val diff = origin._3 - origin._2 //Set the difference to the original x axis minimum subtracted from the original x axis maximum
						if(xAxis0.selected()){ //If we are forcing 0 in range
							if(xAxis.upperBound() > 0d){ //If the maximum is above 0
								xAxis.upperBound() = origin._3 + (origin._1 - me.sceneX) * diff / width() //Change the maximum of the x axis according to the amount scolled
							}
							if(xAxis.lowerBound() < 0d){ //If the minimum is below 0
								xAxis.lowerBound() = origin._2 + (origin._1 - me.sceneX) * diff / width() //Change the minimum of the x axis according to the amount scolled
							}
						}
						else{ //If we are not forcing 0 in range, do the above without the if statements
							xAxis.lowerBound() = origin._2 + (origin._1 - me.sceneX) * diff / width()
							xAxis.upperBound() = origin._3 + (origin._1 - me.sceneX) * diff / width()
						}
					}
					if(!yAxisLock.selected() && !yAxisAR.selected()){ //Same as above but for the y axis
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
			val series = lines.head.zipWithIndex.tail.foldLeft(Seq[(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)]())((acc,sName) => { //Set a value series as equivilent to the result of an accumulation function on each point in the 1st line from stdin zipped (ie in a tuple with) their index with a starting value of an empty sequence of javafx series. The function is as follows:
				val srs = XYChart.Series.sfxXYChartSeries2jfx(new XYChart.Series[Number, Number]{ //Declare a data series with parameters as follows
					name = sName._1 //The name is the 1st part of the zipped tuple (ie the string from stdin)
					data() ++= lines.tail.map(line => { //The data of the series is an array based on the result of a function on each of the remaining lines from std in
						val rtn = XYChart.Data[Number, Number](line(0).toDouble, line(sName._2).toDouble) //Create a datapoint where the x value is the current time (1st string on the line) and the y value is the string at the index of 2nd part of the zipped tuple (the index of the title)
						rtn.setNode(hn(line(0).toDouble, line(sName._2).toDouble)) //Set the node (nodes here are the base class of visual aspect of the program) to display at the datapoint to an invisible box as declared earlier
						rtn //Return the datapoint
					})
				})
				val bChange = BooleanProperty(false) //Create a boolean property (a monitorable boolean) and set the value to initally false
				bChange.onChange { //When the boolean property changes
					if(bChange()){ //If it is changing from false to true
						graph.data() = graph.data() ++= Seq(srs) //Add the series to the graph
					}
					else{ //If it is changing from true to false
						graph.data() = graph.data().filter(it => it != srs) //Filter the series out of the graph data
					}
				}
				acc :+ (srs,bChange) //Append the series and the boolean property to the accumulation series and return it
			})
			val sc = StringConverter[(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)]((s:String) => series.find((ser) => ser._1.name() == s).get, {(v:(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)) => v._1.name()}) //Create a way to convert a series to and from a string. Necessary to display the data series in a list view.
			val lv = new ListView[(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)](series){ //Create a listview with parameters as follows
				def lb = new CheckBoxListCell((item:(javafx.scene.chart.XYChart.Series[Number, Number],BooleanProperty)) => item._2,sc) //Define the method to call when a cell is created.
				cellFactory = ((lv) => lb) //Set the listview to produce cells in the way we just descibed. This works because lb is of type JFX series & boolean property => booleanproperty and stringconverter, so when this is called on a listview it will go through each item (of type dataseries) on the listview, convert it to a string in the way we described and set the booleanproperty to be updated when the box is checked.
				vgrow = Sometimes //Grow the listview vertically if there are no other objects with higher priorty that are wanting to grow.
			}
			val txt = new Text("Selected Waveforms:"){ //Add a new text which tells the user what the listbox is for.
				vgrow = Always
				hgrow = Always
				style = "-fx-font-size:18;" //Set the font size to 18px
			}
			val jpg = new Button("Export to JPG"){ //Make a button that allows the user to export the graph to a JPG file
				hgrow = Always
				alignment = CenterLeft //Align it at the centre left of its parent
				onMouseClicked = (me:MouseEvent) => { //When the mouse is clicked (ie pressed and released quickly)
					val fc = new FileChooser{ //Make a new file selection dialogue object
						title = "Save as JPG" //Set the title of the dialogue
						extensionFilters ++= Seq(new FileChooser.ExtensionFilter("JPG Files", "*.jpg")) //Only show jpg files in the dialogue
					}
					var file = fc.showSaveDialog(stage) //Show the file selection dialogue and save the selection to a file. Note that file is a variable as we will need to change it if ".jpg" has not been added to the end of the file name.
					if(file!=null){ //If the
						val ss = graph.snapshot(new SnapshotParameters, null) //Record the current state of the graph object
						val oImg = fromFXImage(ss,null) //Convert that state to an image
						if(file.toString.dropRight(4) != ".jpg"){ //if the file selected doesn't end with ".jpg"
							file = new java.io.File(file.toString + ".jpg") //Make a new file location with ".jpg" added
						}
						javax.imageio.ImageIO.write(oImg, "JPEG", file) //Write a jpeg to the selected file location.
					}
				}
			}
			val png = new Button("Export to PNG"){ //Same as above but for a PNG
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
			val gp:GridPane = new GridPane(){ //Make a new gridpane with a black border. Gridpanes allow for nodes to be aligned in a grid. I should've used a vbox here but didn't know whether I'd have 2 columns here at the time.
				border() = new Border(new BorderStroke(web("0x000000"),BorderStrokeStyle.Solid,CornerRadii.Empty,BorderWidths.Default)) //Set the border (why so many arguments & includes I literally just wanted a nice border)
			}
			gp.add(xAxisAR,0,0) //Add all our checkboxes to the gridpane
			gp.add(yAxisAR,0,1)
			gp.add(xAxis0,0,2)
			gp.add(yAxis0,0,3)
			gp.add(xAxisLock,0,4)
			gp.add(yAxisLock,0,5)
			gp.add(enableTTs,0,6)
			val hb = new HBox(jpg,png) //Create a new hbox (horizontal box) node with the "export to jpg" and "export to png" buttons as children
			val vb = new VBox(gp,hb,txt,lv) //Create a new vbox (vertical box) node with the gridpane (with the checkboxes), hbox (with the buttons), "selected waveforms" text and the listview as children
			vb.width.onChange{ //When the width of the vbox changes
				hb.minWidth() = vb.width() //Set the hbox to fill the vbox horizontally (setting grow to always didn't always work)
				txt.minWidth(vb.width()) //Set the text to fill the vbox horizontally
				png.prefWidth() = vb.width() / 2.25d //Scale the buttons nicely
				jpg.prefWidth() = vb.width() / 2.25d
				hb.spacing() = vb.width()/10d //Set the spacing between the buttons
				hb.padding() = Insets(vb.width()/180d,0d,vb.width()/180d,0d) //Set padding so that the buttons aren't right at the edge of the screen
			}
			val sp = new StackPane{ //Make a new stackpane node for the graph and cursor location text to sit on
				hgrow = Always //Always grow it horizonally in prefererence to the vbox
				children = Seq(graph,cursorLoc) //Set the graph and cursor location text as the background
			}
			val box = new HBox(vb,sp) //Create a new hbox with the vbox and stackpane as children
            root = box //set the root node of the program as this hbox
        }
    }
}

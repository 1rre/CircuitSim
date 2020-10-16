def voltageImpl(lp: Vector[Mesh]) = {
    lp.foreach(mesh => {
      mesh.components.init.zip(mesh.nodes).foreach(__ => {
        def nd = __._2
        def c = __._1
        if(c.isInstanceOf[VoltageSrc]) nd match {
          case b if b == c.pos && c.pos.voltage.parts.intersect(c.asInstanceOf[VoltageSrc].voltage.parts).length == 0 => {
            c.pos.addVoltage += c.asInstanceOf[VoltageSrc].voltage
            println("added to " + c.pos.id)
            c.pos.vInSource = c.neg
          }
          case b if b == c.neg && c.neg.voltage.parts.intersect(c.asInstanceOf[VoltageSrc].voltage.parts).length == 0 => {
            c.neg.addVoltage += c.asInstanceOf[VoltageSrc].voltage
            println("added to " + c.neg.id)
            c.neg.vInSource = c.pos
          }
          case _ =>
        }
      })
    })
  }
  def impedanceImpl(lp: Vector[Mesh]) = {}
  
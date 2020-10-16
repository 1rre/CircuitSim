import mill._, scalalib._

object circuit extends ScalaModule {
  def scalaVersion = "2.13.3"
  def unmanagedClasspath = T {
    if (!os.exists(millSourcePath / "lib")) Agg()
  	else Agg.from(os.list(millSourcePath / "lib").map(PathRef(_)))
	}
  def scalacOptions = Seq("-deprecation")
}
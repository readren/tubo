package tubo

object Boot {

  def main(args: Array[String]): Unit = {
    akka.Main.main(Array(classOf[Supervisor].getName))
  }

}
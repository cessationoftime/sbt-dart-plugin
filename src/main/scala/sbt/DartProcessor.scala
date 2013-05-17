package sbt



trait DartSdk {
  
  def dart2jsExePath = DartSdk.dart2jsExe.absolutePath
  
  def pubExePath = DartSdk.pubExe.absolutePath
  
  def dartExePath = DartSdk.dartExe.absolutePath
}

object DartSdk {
   lazy val dartSdk: File = {
    
    val DART_SDK = System.getenv("DART_SDK")
    if (DART_SDK == null) {
      sys.error("DART_HOME env variable must be defined!")
    } else {
      val dartHome = new File(DART_SDK)
      if (dartHome.exists())
        dartHome
      else
        sys.error(dartHome + " does not exist!")
    }
  }

  lazy val pubExe: File = {
    val path = dartSdk + "/bin/pub"
    val exe = new File(path)
    if (exe.exists())
      exe
    else
      sys.error(exe + " does not exist!")

  }

  lazy val dart2jsExe: File = {
    val path = dartSdk + "/bin/dart2js"
    val exe = new File(path)
    if (exe.exists())
      exe
    else
      sys.error(exe + " does not exist!")

  }

  lazy val dartExe: File = {
    val path = dartSdk + "/bin/dart"
    val exe = new File(path)
    if (exe.exists())
      exe
    else
      sys.error(exe + " does not exist!")

  }

}

trait DartProcessor extends DartSdk {

  /**
   * Compile dart file into javascript.
   * @param dartFile
   * @param options dart compiler options
   * @return (source, None, Seq(deps))
   */
  def dart2js(webui: Boolean, web: File, module: Option[String], entryPoint: String, options: Seq[String]): (File, File) = {

    val out = if (webui) "out/" else ""

    val entryPointPath = module.map(m => m + "/" + out).getOrElse(out) + entryPoint

    val cmd = dart2jsExePath + " " + options.mkString(" ") + " -o" + entryPointPath + ".js" + " " + entryPointPath

    val dartFile = web / entryPointPath

    import scala.sys.process._
    val d2js = Process(cmd, web)

    var stdout = List[String]()
    var stderr = List[String]()
    val exit = d2js ! ProcessLogger((s) => stdout ::= s, (s) => stderr ::= s)

    if (exit != 0) {
      throw CompilationException(stdout.mkString("\n") + stderr.mkString("\n"), dartFile, None)
    }

    (web / (entryPointPath + ".js"), web / (entryPointPath + ".js.deps"))

  }

  def resolves(public: File, module: Option[String], entryPoint: String): (File, File)

  def compile(webDir: File, module: Option[String], entryPoint: String, public: File, options: Seq[String], dev: Boolean): (File, Option[File])

  def deployables(dev: Boolean, web: File, module: Option[String], entryPoint: String): Seq[String]
}
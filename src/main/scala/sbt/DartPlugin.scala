package sbt

import Keys._
import sbt._
import sbt.Keys._
import play.Project._
import DartKeys._
import sbt.ConfigKey.configurationToKey
import sbt.Project.richInitializeTask
import sbt.Scoped.richFileSetting

object DartPlugin extends Plugin
  with DartPlayAssetDeployer
  with Dart2jsCompiler
  with DartTask {

  
  
  override lazy val settings = Seq(
    //    webuic <<= webuicTask.runBefore(PlayProject.playCopyAssets),
    //    dart2js <<= dart2jsTask.runBefore(PlayProject.playCopyAssets),

    pubInstallTask <<= dartPubInstall.runBefore(PlayProject.playCommonClassloader),
    
    
    unmanagedBase <<= baseDirectory { base => base / "playlibs" },
    
    dartDev := false,  
    dartVerbose := false,  
    dartNoJs := false,
      
    dartPublicManagedResources <<= (resourceManaged in Compile) / "public",
    
    
    dartDirectory <<= baseDirectory,
    dartPackagesDirectory <<= (dartDirectory) / "packages",
    dartWebDirectory <<= (dartDirectory) / "web",
    dartLibDirectory <<= (dartDirectory) / "lib",

    
    dartPublicDirectory <<= baseDirectory / "public",

    resourceDirectories in Compile <+= dartDirectory,
    
    resourceGenerators in Compile <+= dartWebUICompiler,
    resourceGenerators in Compile <+= dartAssetsDeployer,
    resourceGenerators in Compile <+= dart2jsCompiler,

    dartEntryPoints := Seq.empty[String],
    dartWebUIEntryPoints := Seq.empty[String],

    dartOptions := Seq.empty[String])

}


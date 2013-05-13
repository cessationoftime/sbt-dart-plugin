package sbt

import Keys._
import PlayKeys._
import DartKeys._
import PlayExceptions._
import play.api.PlayException
import java.nio.file.Files


trait DartPlayAssetDeployer {

  // ----- Assets

  // Name: name of the compiler
  // files: the function to find files to compile from the assets directory
  // naming: how to name the generated file from the original file and whether it should be minified or not
  // compile: compile the file and return the compiled sources, the minified source (if relevant) and the list of dependencies
  def DartPlayAssetDeployer(name: String,
    watch: File => PathFinder,
    optionsSettings: sbt.SettingKey[Seq[String]]) =
    (state, dartPublicDirectory, dartEntryPoints, dartPackagesDirectory, dartWebDirectory, resourceManaged in Compile, cacheDirectory, optionsSettings, requireJs) map { (state, public, entryPoints, dartPackages, web, resources, cache, options, requireJs) =>

           

        import java.io._

        val cacheFile = cache / name

        val currentInfos = watch(web).get.map(f => f -> FileInfo.lastModified(f)).toMap

        val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)

        if (previousInfo != currentInfos) {

          state.log.info("\t++++ " + name + " ++++ ")

          //a changed file can be either a new file, a deleted file or a modified one
          lazy val changedFiles: Seq[File] = currentInfos.filter(e => !previousInfo.get(e._1).isDefined || previousInfo(e._1).lastModified < e._2.lastModified).map(_._1).toSeq ++ previousInfo.filter(e => !currentInfos.get(e._1).isDefined).map(_._1).toSeq

          //erease dependencies that belong to changed files
          val dependencies = previousRelation.filter((original, compiled) => changedFiles.contains(original))._2s
          dependencies.foreach(IO.delete)

          
          val dartAssets = watch(web)
          
          
        //  val gen = dartAssets x relativeTo(Seq(web))
          
          
          /**
           * If the given file was changed or
           * if the given file was a dependency,
           * otherwise calculate dependencies based on previous relation graph
           */
          val generated: Seq[(File, java.io.File)] = (dartAssets x relativeTo(Seq(web))).filter {case (f,n)=>f.isFile()}.flatMap {
            case (sourceFile, name) => {
              if (changedFiles.contains(sourceFile) ) {
                
                state.log.info("Update: " + sourceFile)
                
                val targetFile = new File(resources, "public/" + name)
                
                IO.copyFile(sourceFile, targetFile, true)
                
                Seq(sourceFile).map(_ -> targetFile) 
              } else {
                previousRelation.filter((original, compiled) => original == sourceFile)._2s.map(sourceFile -> _)
              }
            }
          }

          //write object graph to cache file 
          Sync.writeInfo(cacheFile, Relation.empty[File, File] ++ generated, currentInfos)(FileInfo.lastModified.format)

          // Return new files
          generated.map(_._2).distinct.toList

        } else {
          // Return previously generated files
          previousRelation._2s.toSeq
        }
      
    }

  val dartAssetsDeployer = DartPlayAssetDeployer(dartId + "-dart2dart",
    (base)=>(base ** "*.*" --- (base ** "*.dart")  --- (base **  "packages" ** "*") +++ (base ** "packages" / "browser" ** "*"))  ,
    dartOptions in Compile)

  

}
  

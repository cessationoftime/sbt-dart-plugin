package sbt

import Keys._
import play.Keys._
import DartKeys._
import play.PlayExceptions._
import sbt.ConfigKey.configurationToKey
import sbt.Scoped.t10ToTable10
import sbt.State.stateOps

trait DartPlayAssetDeployer {

  // ----- Assets


  def DartPlayAssetDeployer(name: String,
    optionsSettings: sbt.SettingKey[Seq[String]]) =
    (dartVerbose, dartDev, state, dartPublicDirectory, dartEntryPoints, dartPackagesDirectory, dartWebDirectory, resourceManaged in Compile, cacheDirectory, optionsSettings) map { (verbose, dev, state, public, entryPoints, dartPackages, web, resources, cache, options) =>

      val watch = if (dev)
        (base: File) => (base ** "*.*")
      else
        (base: File) => (base ** "*.*" --- (base ** "*.dart") --- (base ** "*.dart.map") --- (base ** "packages" ** "*") +++ (base ** "packages" / "browser" ** "*"))

      import java.io._

      val cacheFile = cache / name

      val currentInfos = watch(web).get.map(f => f -> FileInfo.lastModified(f)).toMap

      val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)

      if (previousInfo != currentInfos) {
        if (verbose)
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
        val generated: Seq[(File, java.io.File)] = (dartAssets x relativeTo(Seq(web))).filter { case (f, n) => f.isFile() }.flatMap {
          case (sourceFile, name) => {
            if (changedFiles.contains(sourceFile)) {

              if (verbose)
                state.log.info("Update: " + sourceFile)

              val targetFile = new File(resources, "public/" + name)

              IO.copyFile(sourceFile, targetFile, true)

              List((sourceFile, targetFile))
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
    dartOptions in Compile)

}
  

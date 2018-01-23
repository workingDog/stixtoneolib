package com.kodekutters.neo4j

import java.io.{File, InputStream}

import com.kodekutters.stix.{Bundle, _}
import com.typesafe.scalalogging.Logger
import org.slf4j.helpers.NOPLogger
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.{implicitConversions, postfixOps}

/**
  * Read STIX-2 objects from files and load them into a Neo4j database
  */
object Neo4jFileLoader {

  /**
    * read a Bundle from the input source
    *
    * @param source the input InputStream
    * @param logger the optional logger
    * @return a Bundle option
    */
  def readBundle(source: InputStream, logger: Option[Logger] = None): Option[Bundle] = {
    // read a STIX bundle from the InputStream
    val jsondoc = Source.fromInputStream(source).mkString
    Option(Json.parse(jsondoc)) match {
      case None => logger.map(_.error("could not parse JSON in file")); None
      case Some(js) =>
        // create a bundle object from it
        Json.fromJson[Bundle](js).asOpt match {
          case None => logger.map(_.error("ERROR invalid bundle JSON in file")); None
          case Some(bundle) => Option(bundle)
        }
    }
  }

}

/**
  * loads files of Stix-2.0 objects, SDO, SRO and associated data types into a Neo4j graph database
  *
  * @author R. Wathelet June 2017, revised December 2017
  * @param dbDir the neo4j graph database directory name of an existing database or where a new one will be created
  * @param logger the implicit Logger, default to NOPLogger if absent
  */
class Neo4jFileLoader(dbDir: String, hostAddress: String = "localhost:7687")(implicit logger: Logger = Logger(NOPLogger.NOP_LOGGER)) {

  import Neo4jFileLoader._

  /**
    * the STIX-2 Neo4j loader
    */
  val loader = new Neo4jLoader(dbDir, hostAddress)

  /**
    * read a bundle of Stix objects from the input json file,
    * convert it to neo4j nodes and relations and load them into the neo4j db
    *
    * @param inFile the json file containing the bundle of stix objects
    */
  def loadBundleFile(inFile: File): Unit = {
    logger.info("processing file: " + inFile.getCanonicalPath)
    // read a STIX bundle from the input file
    val jsondoc = Source.fromFile(inFile).mkString
    Option(Json.parse(jsondoc)) match {
      case None => logger.error("could not parse JSON in file: " + inFile.getName)
      case Some(js) =>
        // create a bundle object from it and convert its objects to nodes and relations
        Json.fromJson[Bundle](js).asOpt match {
          case None => logger.error("ERROR reading bundle in file: " + inFile.getName)
          case Some(bundle) =>
            loader.loadIntoNeo4j(bundle)
            loader.counter.log()
        }
        loader.close()
    }
  }

  /**
    * read Stix bundles from the input zip file and
    * convert them to neo4j nodes and relations and load them into the neo4j db
    *
    * @param inFile the zip file containing constituent json files each with a bundle of stix objects
    */
  def loadBundleZipFile(inFile: File): Unit = {
    import scala.collection.JavaConverters._
    logger.info("processing file: " + inFile.getCanonicalPath)
    // get the zip file
    val rootZip = new java.util.zip.ZipFile(inFile)
    // for each entry file containing a single bundle
    rootZip.entries.asScala.foreach(f => {
      readBundle(rootZip.getInputStream(f)) match {
        case Some(bundle) =>
          logger.info("file: " + f.getName + " --> " + inFile)
          loader.loadIntoNeo4j(bundle)
          loader.counter.log()
        case None => logger.error("ERROR invalid bundle JSON in zip file: \n")
      }
    })
    loader.close()
  }

  /**
    * For processing very large text files.
    *
    * read Stix objects one by one from the input file,
    * convert them to neo4j nodes and relations and load them into the neo4j db
    *
    * The input file must contain a Stix object on one line ending with a new line.
    *
    */
  def loadLargeTextFile(inFile: File): Unit = {
    logger.info("processing file: " + inFile.getName)
    // go through the file twice, on first pass process the nodes, on second pass relations
    for (pass <- 1 to 2) {
      // read a STIX object from the inFile, one line at a time
      for (line <- Source.fromFile(inFile).getLines) {
        Option(Json.parse(line)) match {
          case None => logger.error("could not parse JSON in file: " + inFile + " line: " + line)
          case Some(js) =>
            // create a Stix object from it
            Json.fromJson[StixObj](js).asOpt match {
              case None => logger.error("ERROR reading StixObj in file: " + inFile + " line: " + line)
              case Some(stixObj) =>
                if (pass == 1)
                  loader.nodesMaker.createNodes(stixObj)
                else
                  loader.relsMaker.createRelations(stixObj)
            }
        }
      }
    }
    loader.close()
  }

  /**
    * For processing very large zip files.
    *
    * read Stix objects one by one from the input zip file,
    * convert them to neo4j nodes and relations and load them into the neo4j db
    *
    * There can be one or more file entries in the zip file,
    * each file must have the extension ".json".
    *
    * Each entry file must have a Stix object on one line ending with a new line.
    *
    */
  def loadLargeZipTextFile(inFile: File): Unit = {
    logger.info("processing file: " + inFile.getName)
    // get the input zip file
    val rootZip = new java.util.zip.ZipFile(inFile)
    // for each entry file
    rootZip.entries.asScala.foreach(f => {
      // go thru the file twice, on first pass process the nodes, on second pass relations
      for (pass <- 1 to 2) {
        // get the lines from the entry file
        val inputLines = Source.fromInputStream(rootZip.getInputStream(f)).getLines
        // read a Stix object from the inputLines, one line at a time
        for (line <- inputLines) {
          Option(Json.parse(line)) match {
            case None => logger.error("could not parse JSON in file: " + inFile + " line: " + line)
            case Some(js) =>
              // create a Stix object from it
              Json.fromJson[StixObj](js).asOpt match {
                case None => logger.error("ERROR reading StixObj in file: " + inFile + " line: " + line)
                case Some(stixObj) =>
                  if (pass == 1)
                    loader.nodesMaker.createNodes(stixObj)
                  else
                    loader.relsMaker.createRelations(stixObj)
              }
          }
        }
      }
    }
    )
    loader.close()
  }

}

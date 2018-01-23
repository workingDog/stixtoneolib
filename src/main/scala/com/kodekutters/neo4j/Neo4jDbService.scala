package com.kodekutters.neo4j

import java.io.File

import com.typesafe.scalalogging.Logger
import org.neo4j.graphdb.{GraphDatabaseService, Node}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.index.Index
import org.neo4j.kernel.configuration.BoltConnector

import scala.util.Try

/**
  * the GraphDatabaseService support and associated index
  */
class Neo4jDbService(dbDir: String, hostAddress: String = "localhost:7687")(implicit logger: Logger) {

  import Neo4jDbService._

  var graphDB: GraphDatabaseService = _

  var idIndex: Index[Node] = _

  // todo
  val pathToConfig = ""

  /**
    * initialise this service, ie start a neo4j database server
    */
  def init(): Unit = {
    // start a neo4j database server
    // will create a new database or open an existing one
    val factory = new GraphDatabaseFactory()
    val bolt = new BoltConnector("bolt-neo-access")
   // Try(graphDB = factory.newEmbeddedDatabase(new File(dbDir))).toOption match {
    Try(
      graphDB = factory.newEmbeddedDatabaseBuilder(new File(dbDir))
        .setConfig(bolt.`type`, "BOLT")
        .setConfig(bolt.enabled, "true")
        .setConfig(bolt.listen_address, hostAddress)
        .newGraphDatabase()
    ).toOption match {
      case None =>
        logger.error("cannot access " + dbDir + ", ensure no other process is using this database, and that the directory is writable")
        throw new IllegalStateException("cannot access " + dbDir)

      case Some(gph) =>
        registerShutdownHook()
        logger.info("connected to Neo4j " + factory.getEdition + " at: " + dbDir)
        transaction {
          idIndex = graphDB.index.forNodes("id")
        }.getOrElse(logger.error("could not process indexing in DbService.init()"))
    }
  }

  /**
    * do a transaction that evaluate correctly to Some(result) or to a failure as None
    *
    * returns an Option
    */
  def transaction[A <: Any](dbOp: => A): Option[A] = Try(plainTransaction(graphDB)(dbOp)).toOption

  def closeAll() = {
    if (graphDB != null) graphDB.shutdown()
  }

  def registerShutdownHook() =
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run = graphDB.shutdown()
    })

}

object Neo4jDbService {

  // general transaction support
  // see snippet: http://sandrasi-sw.blogspot.jp/2012/02/neo4j-transactions-in-scala.html
  private def plainTransaction[A <: Any](db: GraphDatabaseService)(dbOp: => A): A = {
    val tx = synchronized {
      db.beginTx
    }
    try {
      val result = dbOp
      tx.success()
      result
    } finally {
      tx.close()
    }
  }

}
package com.kodekutters.neo4j

import java.io.File

import com.typesafe.scalalogging.Logger

import org.neo4j.graphdb.{GraphDatabaseService, Node}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.index.Index

import scala.util.Try


/**
  * the GraphDatabaseService support and associated index
  */
object Neo4jDbService {

  var graphDB: GraphDatabaseService = _

  var idIndex: Index[Node] = _

  val pathToConfig = ""

  /**
    * initialise this singleton
    *
    * @param dbDir dbDir the directory of the database
    */
  def init(dbDir: String)(implicit logger: Logger): Unit = {
    // start a neo4j database server
    // will create a new database or open the existing one
    val factory = new GraphDatabaseFactory()
    Try(graphDB = factory.newEmbeddedDatabase(new File(dbDir))).toOption match {
      case None =>
        logger.error("cannot access " + dbDir + ", ensure no other process is using this database, and that the directory is writable")
        System.exit(1)
      case Some(gph) =>
        registerShutdownHook()
        logger.info("connected to Neo4j " + factory.getEdition + " at: " + dbDir)
        transaction {
          idIndex = graphDB.index.forNodes("id")
        }.getOrElse(logger.error("could not process indexing in DbService.init()"))
    }
  }

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

  /**
    * do a transaction that evaluate correctly to Some(result) or to a failure as None
    *
    * returns an Option
    */
  def transaction[A <: Any](dbOp: => A): Option[A] = Try(plainTransaction(Neo4jDbService.graphDB)(dbOp)).toOption

  def closeAll() = {
    graphDB.shutdown()
  }

  private def registerShutdownHook() =
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run = graphDB.shutdown()
    })

}

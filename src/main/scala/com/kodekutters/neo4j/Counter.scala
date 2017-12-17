package com.kodekutters.neo4j

import com.typesafe.scalalogging.Logger

import scala.collection.mutable


/**
  * helper class for counting the SDO, SRO and StixObj
  *
  */
case class Counter()(implicit logger: Logger) {

  val count = mutable.Map("SDO" -> 0, "SRO" -> 0, "StixObj" -> 0)

  /**
    * reset the counter to zero
    */
  def reset(): Unit = {
    count.foreach({ case (k, v) => count(k) = 0 })
  }

  /**
    * log the counter information
    */
  def log(): Unit = {
    // print the number of SDO, SRO and StixObj (MarkingDefinition+LanguageContent)
    count.foreach({ case (k, v) => logger.info(k + ": " + v) })
    // sum the SDO, SRO and StixObj
    logger.info("total: " + count.foldLeft(0)(_ + _._2))
  }

  /**
    * increment the count of key=k by 1
    */
  def inc(k: String): Unit = count(k) = count(k) + 1

}

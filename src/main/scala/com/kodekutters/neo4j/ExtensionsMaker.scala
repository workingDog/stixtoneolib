package com.kodekutters.neo4j

import java.util.UUID

import com.kodekutters.stix._
import com.typesafe.scalalogging.Logger
import org.neo4j.graphdb.Label.label
import org.neo4j.graphdb.{Node, RelationshipType}

/**
  * create an Extension node and associated relations
  */
class ExtensionsMaker(neoService: Neo4jDbService) {

  // convenience implicit transformation from a string to a RelationshipType
  implicit def string2relationshipType(x: String): RelationshipType = RelationshipType.withName(x)

  val support = new MakerSupport(neoService)

  /**
    * create the Extension nodes and embedded relations for the Observable object
    *
    * @param sourceNode the Neo4j node the Observable the Extension belongs to
    * @param extMapOpt  the map of Extensions
    * @param ext_ids    the map of Extensions ids
    */
  def create(sourceNode: Node, extMapOpt: Option[Extensions], ext_ids: Map[String, String])(implicit logger: Logger) = {
    extMapOpt.foreach(extMap => {
      // for each extension
      for ((k, extention) <- extMap.extensions) {
        // create the Extension node
        val xNodeOpt = neoService.transaction {
          val node = neoService.graphDB.createNode(label(support.asCleanLabel(k)))
          node.addLabel(label("Extension"))
          node.setProperty("extension_id", ext_ids(k))
          node
        }
        xNodeOpt match {
          case Some(xNode) =>
            // create a relation between the parent Observable node and this Extension node
            neoService.transaction {
              sourceNode.createRelationshipTo(xNode, "HAS_EXTENSION")
            }.getOrElse {logger.error("could not process HAS_EXTENSION relation"); Unit}

            // add the specific attributes to the extension node
            extention match {
              case x: ArchiveFileExt =>
                neoService.transaction {
                  xNode.setProperty("contains_refs", x.contains_refs.getOrElse(List.empty).toArray)
                  xNode.setProperty("version", x.version.getOrElse(""))
                  xNode.setProperty("comment", x.comment.getOrElse(""))
                }

              case x: NTFSFileExt =>
                val altStream_ids = support.toIdArray(x.alternate_data_streams)
                neoService.transaction {
                  xNode.setProperty("sid", x.sid.getOrElse(""))
                  xNode.setProperty("alternate_data_streams", altStream_ids)
                }
                createAltDataStream(xNode, x.alternate_data_streams, altStream_ids)

              case x: PdfFileExt =>
                neoService.transaction {
                  xNode.setProperty("version", x.version.getOrElse(""))
                  xNode.setProperty("is_optimized", x.is_optimized.getOrElse(false))
                  xNode.setProperty("pdfid0", x.pdfid0.getOrElse(""))
                  xNode.setProperty("pdfid1", x.pdfid1.getOrElse(""))
                }

              case x: RasterImgExt =>
                val exitTags_ids: Map[String, String] = (for (s <- x.exif_tags.getOrElse(Map.empty).keySet) yield s -> UUID.randomUUID().toString).toMap
                neoService.transaction {
                  xNode.setProperty("image_height", x.image_height.getOrElse(0))
                  xNode.setProperty("image_width", x.image_width.getOrElse(0))
                  xNode.setProperty("bits_per_pixel", x.bits_per_pixel.getOrElse(0))
                  xNode.setProperty("exif_tags", exitTags_ids.values.toArray)
                  xNode.setProperty("image_compression_algorithm", x.image_compression_algorithm.getOrElse(""))
                }
                createExifTags(xNode, x.exif_tags, exitTags_ids)

              case x: WindowPEBinExt =>
                neoService.transaction {
                  xNode.setProperty("pe_type", x.pe_type)
                  xNode.setProperty("imphash", x.imphash.getOrElse(""))
                  xNode.setProperty("machine_hex", x.machine_hex.getOrElse(""))
                  xNode.setProperty("number_of_sections", x.number_of_sections.getOrElse(0))
                  xNode.setProperty("time_date_stamp", x.time_date_stamp.getOrElse("").toString)
                  xNode.setProperty("pointer_to_symbol_table_hex", x.pointer_to_symbol_table_hex.getOrElse(""))
                  xNode.setProperty("number_of_symbols", x.number_of_symbols.getOrElse(0))
                  xNode.setProperty("size_of_optional_header", x.size_of_optional_header.getOrElse(0))
                  xNode.setProperty("characteristics_hex", x.characteristics_hex.getOrElse(""))
                  // todo file_header_hashes
                  // todo optional_header
                  // todo sections
                }

              case _ =>
            }

          case None => logger.error("could not create node Extension")
        }

      }
    })
  }

  private def createAltDataStream(fromNode: Node, altStreamOpt: Option[List[AlternateDataStream]], ids: Array[String])(implicit logger: Logger) = {
    altStreamOpt.foreach(altStream => {
      for ((kp, i) <- altStream.zipWithIndex) {
        val hashes_ids: Map[String, String] = (for (s <- kp.hashes.getOrElse(Map.empty).keySet) yield s -> UUID.randomUUID().toString).toMap
        val tgtNodeOpt = neoService.transaction {
          val node = neoService.graphDB.createNode(label(kp.name))
          node.setProperty("alternate_data_stream_id", ids(i))
          node.setProperty("name", kp.name)
          node.setProperty("size", kp.size.getOrElse(0))
          node.setProperty("hashes", hashes_ids.values.toArray)
          node
        }
        tgtNodeOpt.foreach(tgtNode => {
          support.createHashes(tgtNode, kp.hashes, hashes_ids)
          neoService.transaction {
            fromNode.createRelationshipTo(tgtNode, "HAS_ALTERNATE_DATA_STREAM")
          }.getOrElse {logger.error("could not process HAS_ALTERNATE_DATA_STREAM relation"); Unit}
        })
      }
    })
  }

  private def createExifTags(fromNode: Node, exitTagsOpt: Option[Map[String, Either[Long, String]]], ids: Map[String, String])(implicit logger: Logger) = {
    exitTagsOpt.foreach(exitTags =>
      for ((k, obs) <- exitTags) {
        // either a int or string
        val theValue = obs match {
          case Right(x) => x
          case Left(x) => x
        }
        val tgtNodeOpt = neoService.transaction {
          val node = neoService.graphDB.createNode(label(support.asCleanLabel("exif_tags")))
          node.setProperty("exif_tags_id", ids(k))
          node.setProperty(k, theValue)
          node
        }
        tgtNodeOpt.foreach(tgtNode => {
          neoService.transaction {
            fromNode.createRelationshipTo(tgtNode, "HAS_EXIF_TAGS")
          }.getOrElse {logger.error("could not process HAS_EXIF_TAGS relation"); Unit}
        })
      }
    )
  }

}

package com.kodekutters.neo4j

import java.util.UUID

import com.kodekutters.stix._
import com.typesafe.scalalogging.Logger
import org.neo4j.graphdb.Label.label
import org.slf4j.helpers.NOPLogger


/**
  * create Neo4j nodes and internal relations from a STIX-2 object
  *
  */
class NodesMaker(counter: Option[Counter] = None, neoService: Neo4jDbService)(implicit logger: Logger = Logger(NOPLogger.NOP_LOGGER)) {

  val support = new MakerSupport(neoService)
  val observablesMaker = new ObservablesMaker(neoService)

  /**
    * create nodes and embedded relations from a Stix object
    *
    * @param obj the stix object (SDO, SRO or STixObj) from which to create Neo4j nodes
    */
  def createNodes(obj: StixObj) = {
    obj match {
      case stix: SDO => createSDONode(stix)
      case stix: SRO => createSRONode(stix)
      case stix: StixObj => createStixObjNode(stix)
      case _ => // do nothing for now
    }
  }

  /**
    * create nodes and embedded relations from a SDO
    *
    * @param x the SDO from which to create Neo4j nodes
    */
  def createSDONode(x: SDO) = {
    counter.map(_.inc("SDO"))
    // common elements
    val granular_markings_ids = support.toIdArray(x.granular_markings)
    val external_references_ids = support.toIdArray(x.external_references)
    // create a base node and internal relations common to all SDO
    // this includes making a node for a CustomStix object
    val sdoNodeOpt = neoService.transaction {
      val node = neoService.graphDB.createNode(label(support.asCleanLabel(x.`type`)))
      node.addLabel(label("SDO"))
      node.setProperty("id", x.id.toString())
      node.setProperty("type", x.`type`)
      node.setProperty("created", x.created.time)
      node.setProperty("modified", x.modified.time)
      node.setProperty("revoked", x.revoked.getOrElse(false))
      node.setProperty("labels", x.labels.getOrElse(List.empty).toArray)
      //  node.setProperty("confidence", x.confidence.getOrElse(0))
      node.setProperty("external_references", external_references_ids)
      //  node.setProperty("lang", x.lang.getOrElse(""))
      node.setProperty("object_marking_refs", support.toIdStringArray(x.object_marking_refs))
      node.setProperty("granular_markings", granular_markings_ids)
      node.setProperty("created_by_ref", x.created_by_ref.getOrElse("").toString)
      node.setProperty("custom", support.asJsonString(x.custom))
      neoService.idIndex.add(node, "id", node.getProperty("id"))
      node
    }
    // catch any errors
    sdoNodeOpt match {
      case Some(sdoNode) =>
        // create the external_references nodes and relations
        support.createExternRefs(sdoNode, x.external_references, external_references_ids)
        // create the granular_markings nodes and relations
        support.createGranulars(sdoNode, x.granular_markings, granular_markings_ids)
        // for the specific SDO type
        x.`type` match {

          case AttackPattern.`type` =>
            val y = x.asInstanceOf[AttackPattern]
            val kill_chain_phases_ids = support.toIdArray(y.kill_chain_phases)
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("description", y.description.getOrElse(""))
              sdoNode.setProperty("kill_chain_phases", kill_chain_phases_ids)
            }.getOrElse(logger.error("could not process AttackPattern node: " + x.id.toString()))
            support.createKillPhases(sdoNode, y.kill_chain_phases, kill_chain_phases_ids)

          case Identity.`type` =>
            val y = x.asInstanceOf[Identity]
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("identity_class", y.identity_class)
              sdoNode.setProperty("sectors", y.sectors.getOrElse(List.empty).toArray)
              sdoNode.setProperty("contact_information", y.contact_information.getOrElse(""))
              sdoNode.setProperty("description", y.description.getOrElse(""))
            }.getOrElse(logger.error("could not process Identity node: " + x.id.toString()))

          case Campaign.`type` =>
            val y = x.asInstanceOf[Campaign]
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("objective", y.objective.getOrElse(""))
              sdoNode.setProperty("aliases", y.aliases.getOrElse(List.empty).toArray)
              sdoNode.setProperty("first_seen", y.first_seen.getOrElse("").toString)
              sdoNode.setProperty("last_seen", y.last_seen.getOrElse("").toString)
              sdoNode.setProperty("description", y.description.getOrElse(""))
            }.getOrElse(logger.error("could not process Campaign node: " + x.id.toString()))

          case CourseOfAction.`type` =>
            val y = x.asInstanceOf[CourseOfAction]
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("description", y.description.getOrElse(""))
            }.getOrElse(logger.error("could not process CourseOfAction node: " + x.id.toString()))

          case IntrusionSet.`type` =>
            val y = x.asInstanceOf[IntrusionSet]
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("description", y.description.getOrElse(""))
              sdoNode.setProperty("aliases", y.aliases.getOrElse(List.empty).toArray)
              sdoNode.setProperty("first_seen", y.first_seen.getOrElse("").toString)
              sdoNode.setProperty("last_seen", y.last_seen.getOrElse("").toString)
              sdoNode.setProperty("goals", y.goals.getOrElse(List.empty).toArray)
              sdoNode.setProperty("resource_level", y.resource_level.getOrElse(""))
              sdoNode.setProperty("primary_motivation", y.primary_motivation.getOrElse(""))
              sdoNode.setProperty("secondary_motivations", y.secondary_motivations.getOrElse(List.empty).toArray)
            }.getOrElse(logger.error("could not process IntrusionSet node: " + x.id.toString()))

          case Malware.`type` =>
            val y = x.asInstanceOf[Malware]
            val kill_chain_phases_ids = support.toIdArray(y.kill_chain_phases)
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("description", y.description.getOrElse(""))
              sdoNode.setProperty("kill_chain_phases", kill_chain_phases_ids)
            }.getOrElse(logger.error("could not process Malware node: " + x.id.toString()))
            support.createKillPhases(sdoNode, y.kill_chain_phases, kill_chain_phases_ids)

          case Report.`type` =>
            val y = x.asInstanceOf[Report]
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("published", y.published.time)
              sdoNode.setProperty("object_refs_ids", support.toIdStringArray(Option(y.object_refs)))
              sdoNode.setProperty("description", y.description.getOrElse(""))
            }.getOrElse(logger.error("could not process Report node: " + x.id.toString()))

          case ThreatActor.`type` =>
            val y = x.asInstanceOf[ThreatActor]
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("description", y.description.getOrElse(""))
              sdoNode.setProperty("aliases", y.aliases.getOrElse(List.empty).toArray)
              sdoNode.setProperty("roles", y.roles.getOrElse(List.empty).toArray)
              sdoNode.setProperty("goals", y.goals.getOrElse(List.empty).toArray)
              sdoNode.setProperty("sophistication", y.sophistication.getOrElse(""))
              sdoNode.setProperty("resource_level", y.resource_level.getOrElse(""))
              sdoNode.setProperty("primary_motivation", y.primary_motivation.getOrElse(""))
              sdoNode.setProperty("secondary_motivations", y.secondary_motivations.getOrElse(List.empty).toArray)
              sdoNode.setProperty("personal_motivations", y.personal_motivations.getOrElse(List.empty).toArray)
            }.getOrElse(logger.error("could not process ThreatActor node: " + x.id.toString()))

          case Tool.`type` =>
            val y = x.asInstanceOf[Tool]
            val kill_chain_phases_ids = support.toIdArray(y.kill_chain_phases)
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("description", y.description.getOrElse(""))
              sdoNode.setProperty("kill_chain_phases", kill_chain_phases_ids)
              sdoNode.setProperty("tool_version", y.tool_version.getOrElse(""))
            }.getOrElse(logger.error("could not process Tool node: " + x.id.toString()))
            support.createKillPhases(sdoNode, y.kill_chain_phases, kill_chain_phases_ids)

          case Vulnerability.`type` =>
            val y = x.asInstanceOf[Vulnerability]
            neoService.transaction {
              sdoNode.setProperty("name", y.name)
              sdoNode.setProperty("description", y.description.getOrElse(""))
            }.getOrElse(logger.error("could not process Vulnerability node: " + x.id.toString()))

          case Indicator.`type` =>
            val y = x.asInstanceOf[Indicator]
            val kill_chain_phases_ids = support.toIdArray(y.kill_chain_phases)
            neoService.transaction {
              sdoNode.setProperty("name", y.name.getOrElse(""))
              sdoNode.setProperty("description", y.description.getOrElse(""))
              sdoNode.setProperty("pattern", y.pattern)
              sdoNode.setProperty("valid_from", y.valid_from.toString())
              sdoNode.setProperty("valid_until", y.valid_until.getOrElse("").toString)
              sdoNode.setProperty("kill_chain_phases", kill_chain_phases_ids)
            }.getOrElse(logger.error("could not process Indicator node: " + x.id.toString()))
            support.createKillPhases(sdoNode, y.kill_chain_phases, kill_chain_phases_ids)

          case ObservedData.`type` =>
            val y = x.asInstanceOf[ObservedData]
            val obs_ids: Map[String, String] = for (s <- y.objects) yield s._1 -> UUID.randomUUID().toString
            neoService.transaction {
              sdoNode.setProperty("first_observed", y.first_observed.toString())
              sdoNode.setProperty("last_observed", y.last_observed.toString())
              sdoNode.setProperty("number_observed", y.number_observed)
              sdoNode.setProperty("objects", obs_ids.values.toArray)
              sdoNode.setProperty("description", y.description.getOrElse(""))
            }.getOrElse(logger.error("could not process ObservedData node: " + x.id.toString()))
            // create the Observable objects nodes and relations for this ObservedData object
            observablesMaker.create(sdoNode, y.objects, obs_ids)

          case _ => // do nothing here

        }

      case None => logger.error("could not process SDO node: " + x.id.toString())
    }
  }

  /**
    * create an artificial Relationship or a Sighting node so that the embedded relations can refer to it
    *
    * @param x the SRO from which to create Neo4j node
    */
  def createSRONode(x: SRO) = {
    counter.map(_.inc("SRO"))
    neoService.transaction {
      val node = neoService.graphDB.createNode(label("SRO"))
      node.addLabel(label(support.asCleanLabel(x.`type` + "_node")))
      node.setProperty("id", x.id.toString())
      node.setProperty("type", x.`type`) // todo should this be here
      neoService.idIndex.add(node, "id", node.getProperty("id"))
    }.getOrElse(logger.error("could not process SRO node: " + x.id.toString()))
  }

  /**
    * create a Neo4j node from either a MarkingDefinition or LanguageContent object
    *
    * @param stixObj the objects representing a MarkingDefinition or LanguageContent
    */
  def createStixObjNode(stixObj: StixObj) = {
    counter.map(_.inc("StixObj"))
    stixObj match {

      case x: MarkingDefinition =>
        val definition_id = UUID.randomUUID().toString
        val granular_markings_ids = support.toIdArray(x.granular_markings)
        val external_references_ids = support.toIdArray(x.external_references)
        val stixNodeOpt = neoService.transaction {
          val node = neoService.graphDB.createNode(label(support.asCleanLabel(x.`type`)))
          node.addLabel(label("StixObj"))
          node.setProperty("id", x.id.toString())
          node.setProperty("type", x.`type`)
          node.setProperty("created", x.created.time)
          node.setProperty("definition_type", x.definition_type)
          node.setProperty("definition_id", definition_id)
          node.setProperty("external_references", external_references_ids)
          node.setProperty("object_marking_refs", support.toIdStringArray(x.object_marking_refs))
          node.setProperty("granular_markings", granular_markings_ids)
          node.setProperty("created_by_ref", x.created_by_ref.getOrElse("").toString)
          node.setProperty("custom", support.asJsonString(x.custom))
          neoService.idIndex.add(node, "id", node.getProperty("id"))
          node
        }
        // catch errors
        stixNodeOpt match {
          case Some(stixNode) =>
            // the external_references
            support.createExternRefs(stixNode, x.external_references, external_references_ids)
            // the granular_markings
            support.createGranulars(stixNode, x.granular_markings, granular_markings_ids)
            // the marking object definition
            support.createMarkingDef(stixNode, x.definition, definition_id)

          case None => logger.error("could not process MarkingDefinition node: " + x.id.toString())
        }

      case x: LanguageContent =>
        val granular_markings_ids = support.toIdArray(x.granular_markings)
        val external_references_ids = support.toIdArray(x.external_references)
        val lang_contents_ids: Map[String, String] = (for (s <- x.contents.keySet) yield s -> UUID.randomUUID().toString).toMap
        val stixNodeOpt = neoService.transaction {
          val node = neoService.graphDB.createNode(label(support.asCleanLabel(x.`type`)))
          node.addLabel(label("StixObj"))
          node.setProperty("id", x.id.toString())
          node.setProperty("type", x.`type`)
          node.setProperty("created", x.created.time)
          node.setProperty("modified", x.modified.time)
          node.setProperty("object_modified", x.object_modified.time)
          node.setProperty("object_ref", x.object_ref.toString())
          node.setProperty("labels", x.labels.getOrElse(List.empty).toArray)
          node.setProperty("revoked", x.revoked.getOrElse(false))
          node.setProperty("external_references", external_references_ids)
          node.setProperty("object_marking_refs", support.toIdStringArray(x.object_marking_refs))
          node.setProperty("granular_markings", granular_markings_ids)
          node.setProperty("created_by_ref", x.created_by_ref.getOrElse("").toString)
          node.setProperty("contents", lang_contents_ids.values.toArray)
          node.setProperty("custom", support.asJsonString(x.custom))
          neoService.idIndex.add(node, "id", node.getProperty("id"))
          node
        }
        // catch errors
        stixNodeOpt match {
          case Some(stixNode) =>
            // the external_references
            support.createExternRefs(stixNode, x.external_references, external_references_ids)
            // the granular_markings
            support.createGranulars(stixNode, x.granular_markings, granular_markings_ids)
            // the language contents
            support.createLangContents(stixNode, x.contents, lang_contents_ids)

          case None => logger.error("could not process LanguageContent node: " + x.id.toString())
        }
    }
  }

}

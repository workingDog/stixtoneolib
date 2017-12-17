## Stix-2.0 objects to Neo4J conversion

The conversion of STIX-2 objects to a Neo4j representation aims at 
preserving all information present in the STIX-2 objects.

### Terminology used 

The major categories of STIX-2 objects in this document consist of the following:

***SDO***

    AttackPattern, Identity, Campaign, CourseOfAction, IntrusionSet, Malware, 
    Report, ThreatActor, Tool, Vulnerability, Indicator, ObservedData and CustomStix.

***SRO***

    Relationship and Sighting

***StixObj*** 

    MarkingDefinition (and LanguageContent)

Note that in the code all SDO and SRO objects are also StixObj, but only MarkingDefinition (and LanguageContent) 
are exclusively of type StixObj.

### Nodes creation

Every STIX-2 objects has a Neo4j node created for it with all its properties represented.


#### Nodes labeling

The primary Neo4j node LABEL for all SDO objects is its type, for example "indicator".
In addition the label "SDO" is added to all SDO objects.

Since Neo4j does not allow a number of
characters in a Neo4j LABEL, the characters
 
    . , : ' ; " \ \n \r are replaced by " " (a blank character).
    
The character "-" which often appears in STIX-2 types is replaced by "_" in Neo4j LABEL.
This process of replacing those characters for a valid Neo4j LABEL is called *cleaned* in this document.

An "SRO" label is used for the artificial relationship and sighting objects nodes.
These nodes are created so that embedded relations can refer to them.
The label consisting of the SRO type plus the suffix "_node" is also added.
For example, for a Relationship SRO; the label "SRO" and the additional label "relationship_node" are used.

The primary node label for a StixObj object (MarkingDefinition), is its *cleaned* type; e.g. "marking_definition".
In addition the label "StixObj" is added to all StixObj nodes.

#### Embedded relations

SDO objects have a number of internal constituent data types, for example all have a list of
common properties that include a possible external reference object. 
A node plus an embedded relation are
created for each constituent data types. A random ID string is generated
for each and a relation is created from the parent
SDO to that data type node with the generated random ID. When an array of such data types is present
an array of random ID strings is generated. 

The Neo4j LABEL of the node created for those constituent data types consists of its *cleaned* type,
for example: "external_reference". The name given to those embedded relations are in capital letters, such as: "HAS_EXTERNAL_REF",
"HAS_GRANULAR_MARKING", "HAS_MARKING_OBJECT", "HAS_KILL_CHAIN_PHASE", "CREATED_BY" etc...

### Relations creation

Every STIX-2 SRO has a Neo4j relation created for it with all its properties represented.

An SRO of the type *relationship* is represented in Neo4j by a relation from the source to the target SDO 
node id. The id (index) are derived from the source_ref and target_ref properties of the *relationship* SRO. 
The Neo4j relation created holds the full set of properties of the SRO.
Similar to a SDO node, an SRO relation may have embedded relations. These are 
generated as described before.

For *sighting* type SRO, the corresponding Neo4j relation is derived from the *sighting_of_ref* property, 
used as both the source and the target of the relation. The name of the Neo4j relation is set to *sighting_of*.

#### Relations labeling

The name of the Neo4j relation representing an SRO is made from the SRO *cleaned* relationship_type property.


### Observable and Extension nodes and relations

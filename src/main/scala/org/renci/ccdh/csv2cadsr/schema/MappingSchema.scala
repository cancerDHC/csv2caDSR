package org.renci.ccdh.csv2cadsr.schema

import org.json4s._

/**
 * A MappingSchema records all the information needed to map an input CSV file into the Portable Format
 * for Biomedical Data (PFB) and/or CEDAR element formats.
 *
 * There are three types of data we want to capture in this JSON Schema:
 *  - Basic metadata: field name, description field, and so on.
 *  - Semantic mapping: what does this field mean? Can it be represented by a caDSR value?
 *  - JSON Schema typing information: is it a string, number, enum? What are the enumerated values?
 *  - Value mapping: how can the enumerated values be mapped to concepts?
 */
case class MappingSchema(fields: Seq[MappingField]) {
  def asJsonSchema: JObject = {
    val fieldEntries = fields map { field => (field.name, field.asJsonSchema) }

    JObject(
      "properties" -> JObject(fieldEntries.toList),
      "required" -> JArray(fields.filter(_.required).map(_.name).map(JString).toList)
    )
  }
}

object MappingSchema {
  val empty = MappingSchema(Seq())
}

/**
 * A MappingField contains information on each field in a mapping.
 */
abstract class MappingField(val name: String, val uniqueValues: Set[String], val required: Boolean = false) {
  override def toString: String = {
    s"${getClass.getSimpleName}(${name} with ${uniqueValues.size} unique values)"
  }

  def asJsonSchema: JObject
}

object MappingField {
  /**
   * If the number of unique values are less than this proportion of all values,
   * we consider this field to be an enum.
   */
  final val enumProportion = 0.1

  def createFromValues(name: String, values: Seq[String]): MappingField = {
    val uniqueExampleValues = values.toSet

    // Mark this property as required if we have no blanks in the values.
    val isRequired = !(values exists { v => v.isBlank })

    uniqueExampleValues match {
      case _ if (uniqueExampleValues.isEmpty) => EmptyField(name, isRequired)
      case _ if (uniqueExampleValues.size < values.size * enumProportion) => EnumField(name, uniqueExampleValues, isRequired)
      case _ if (uniqueExampleValues forall {str => str forall Character.isDigit}) => {
        val intValues = values flatMap (_.toIntOption)
        IntField(name, uniqueExampleValues, isRequired, Range.inclusive(intValues.min, intValues.max))
      }
      case _ => StringField(name, uniqueExampleValues, isRequired)
    }
  }
}

/*
 * We support Required fields.
 */
class Required {
  def isRequired: Boolean = true
}

/*
 * We support different kinds of mapping fields.
 */
case class StringField(
                        override val name: String,
                        override val uniqueValues: Set[String],
                        override val required: Boolean = false
                      ) extends MappingField(name, uniqueValues) {
  override def asJsonSchema: JObject = JObject(
    "type" -> JString("string"),
    "description" -> JString("")
  )
}
case class EnumField(
                      override val name: String,
                      override val uniqueValues: Set[String],
                      override val required: Boolean = false
                    ) extends MappingField(name, uniqueValues) {
  override def toString: String = {
    s"${getClass.getSimpleName}(${name} with ${uniqueValues.size} unique values: ${uniqueValues.mkString(", ")})"
  }

  override def asJsonSchema: JObject = JObject(
    "type" -> JString("string"),
    "description" -> JString("")
  )
}
case class IntField(
                     override val name: String,
                     override val uniqueValues: Set[String],
                     override val required: Boolean,
                     range: Range
                   ) extends MappingField(name, uniqueValues) {
  override def toString: String = {
    s"${getClass.getSimpleName}(${name} with ${uniqueValues.size} unique values in ${range})"
  }

  override def asJsonSchema: JObject = JObject(
    "type" -> JString("string"),
    "description" -> JString("")
  )
}
case class EmptyField(
                       override val name: String,
                       override val required: Boolean
                     ) extends MappingField(name, Set()) {
  override def asJsonSchema: JObject = JObject(
    "type" -> JString("string"),
    "description" -> JString("")
  )
}
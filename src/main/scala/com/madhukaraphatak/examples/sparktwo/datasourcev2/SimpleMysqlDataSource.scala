package com.madhukaraphatak.examples.sparktwo.datasourcev2.simplemysql

import org.apache.spark.sql.sources.v2._
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.apache.spark.sql.sources.v2.reader._

import scala.collection.JavaConverters._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources._

class DefaultSource extends DataSourceV2 with ReadSupport {

  def createReader(options: DataSourceOptions) = {
    new SimpleMysqlDataSourceReader()
  }
}

class SimpleMysqlDataSourceReader()
    extends DataSourceReader
    with SupportsPushDownFilters {

  var pushedFilters: Array[Filter] = Array[Filter]()
  def readSchema() = {
    val columnNames = Array("user")
    val structFields = columnNames.map(value ⇒ StructField(value, StringType))
    StructType(structFields)
  }

  def pushFilters(filters: Array[Filter]) = {
    println("Filters " + filters.toList)
    pushedFilters = filters
    pushedFilters
  }

  def planInputPartitions = {
    val sparkContext = SparkSession.builder.getOrCreate().sparkContext

    val factoryList = new java.util.ArrayList[InputPartition[InternalRow]]
    factoryList.add(new SimpleMysqlInputPartition(pushedFilters))
    factoryList
  }

}

class SimpleMysqlInputPartition(pushedFilters: Array[Filter])
    extends InputPartition[InternalRow] {

  def createPartitionReader = new SimpleMysqlInputPartitionReader(pushedFilters: Array[Filter])
}

class SimpleMysqlInputPartitionReader(pushedFilters: Array[Filter])
    extends InputPartitionReader[InternalRow] {

  var customIterator: Iterator[Row] = null

  val getQuery: String = {
    if (pushedFilters == null || pushedFilters.isEmpty)
      "(select user from user)a"
    else {
      pushedFilters(1) match {
        case filter: EqualTo =>
          val condition = s"${filter.attribute} = '${filter.value}'"
          s"(select user from user where $condition)a"
        case _ => "(select user from user)a"
      }
    }
  }

  def next = {
    if (customIterator == null) {
      val url = "jdbc:mysql://localhost/mysql"
      val user = "root"
      val password = "abc123"

      val properties = new java.util.Properties()
      properties.setProperty("user", user)
      properties.setProperty("password", password)

      val sparkSession = SparkSession.builder.getOrCreate()
      val df = sparkSession.read.jdbc(url, getQuery, properties)
      val rdd = df.rdd
      val partition = rdd.partitions(0)
      customIterator = rdd.iterator(partition, org.apache.spark.TaskContext.get())
    }
    customIterator.hasNext
  }

  def get = {
    InternalRow(customIterator.next().toSeq)
  }
  def close() = Unit
}

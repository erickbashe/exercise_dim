package com.madhukaraphatak.examples.sparktwo.datasourcev2.simplemysqlwriter

import org.apache.spark.sql.sources.v2._
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.apache.spark.sql.sources.v2.writer._

import scala.collection.JavaConverters._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.sources._
import java.util.Optional

import org.apache.spark.sql.SaveMode
import java.sql.{Connection, DriverManager}

import org.apache.spark.sql.catalyst.InternalRow

class DefaultSource extends DataSourceV2 with WriteSupport {

  def createWriter(jobId: String, schema: StructType, mode: SaveMode,
                   options: DataSourceOptions): Optional[DataSourceWriter] = {
    Optional.of(new MysqlDataSourceWriter())

  }
}

class MysqlDataSourceWriter extends DataSourceWriter {

  override def createWriterFactory(): DataWriterFactory[InternalRow] = {
    new MysqlDataWriterFactory()
  }

  override def commit(messages: Array[WriterCommitMessage]) = {

  }

  override def abort(messages: Array[WriterCommitMessage]) = {

  }

}

class MysqlDataWriterFactory extends DataWriterFactory[InternalRow] {
  override def createDataWriter(partitionId: Int, taskId:Long,epochId: Long): DataWriter[InternalRow] = {
    new MysqlDataWriter()
  }
}

class MysqlDataWriter extends DataWriter[InternalRow] {

  val url = "jdbc:mysql://localhost/test"
  val user = "root"
  val password = "abc123"
  val table ="userwrite"

  val connection = DriverManager.getConnection(url,user,password)
  val statement = "insert into userwrite (user) values (?)"
  val preparedStatement = connection.prepareStatement(statement)


  def write(record: InternalRow) = {
   val value = record.getString(0)
   preparedStatement.setString(1,value)
   preparedStatement.executeUpdate()
  }

  def commit(): WriterCommitMessage = {
    WriteSucceeded
  }

  def abort() = {

  }

  object WriteSucceeded extends WriterCommitMessage

}


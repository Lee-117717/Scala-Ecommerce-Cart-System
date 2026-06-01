package com.ecommerce.dao

import com.ecommerce.entity.Order
import slick.jdbc.MySQLProfile.api._
import java.sql.Timestamp // 导入 Timestamp

class OrderTable(tag: Tag) extends Table[Order](tag, "orders") {
  def id = column[String]("id", O.PrimaryKey)
  def totalAmount = column[BigDecimal]("total_amount", O.SqlType("DECIMAL(10,2)"))
  def createTime = column[Timestamp]("create_time")
  def status = column[String]("status", O.Default("待支付"))
  // 映射元组与 Order 实体一致
  override def * = (id, totalAmount, createTime, status) <> (Order.tupled, Order.unapply)
}

object OrderDAO {
  val query = TableQuery[OrderTable]
}
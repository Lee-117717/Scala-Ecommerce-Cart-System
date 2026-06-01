package com.ecommerce.schema

import com.ecommerce.entity.Order
import slick.jdbc.MySQLProfile.api._
import java.sql.Timestamp

/**
 * 订单表映射类（对应数据库表 orders）
 * 仅在此文件定义，其他文件不再重复
 */
class OrderTable(tag: Tag) extends Table[Order](tag, "orders") {
  def id = column[String]("id", O.PrimaryKey)
  def totalAmount = column[BigDecimal]("total_amount", O.SqlType("DECIMAL(10,2)"))
  def createTime = column[Timestamp]("create_time")
  def status = column[String]("status", O.Default("待支付"))

  // 表行与Order实体双向映射（元组顺序与实体构造函数完全一致）
  override def * = (id, totalAmount, createTime, status) <> (Order.tupled, Order.unapply)

}

// 伴生对象：提供表查询入口（供DAO层调用）
object OrderTable {
  val query = TableQuery[OrderTable]
}
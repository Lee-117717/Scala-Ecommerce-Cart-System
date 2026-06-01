package com.ecommerce.schema

import com.ecommerce.entity.Product
import slick.jdbc.MySQLProfile.api._
import scala.math.BigDecimal

/**
 * 商品表映射类（对应数据库表 products）
 * 与Product实体（id、name、price、quantity）完全对齐
 */
class ProductTable(tag: Tag) extends Table[Product](tag, "products") {
  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def price = column[BigDecimal]("price", O.SqlType("DECIMAL(10,2)"))
  def quantity = column[Int]("quantity")

  // 双向映射：元组顺序与Product构造函数一致
  override def * = (id, name, price, quantity) <> (Product.tupled, Product.unapply)
}

// 伴生对象：供DAO层直接调用，避免重复创建TableQuery
object ProductTable {
  val query = TableQuery[ProductTable]
}
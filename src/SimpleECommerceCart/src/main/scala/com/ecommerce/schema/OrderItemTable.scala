package com.ecommerce.schema
import com.ecommerce.entity.OrderItem
import slick.jdbc.MySQLProfile.api._

class OrderItemTable(tag: Tag) extends Table[OrderItem](tag, "order_items") {
  // 字段与OrderItem实体完全一致
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[String]("order_id")
  def productId = column[String]("product_id")
  def quantity = column[Int]("quantity")
  def price = column[BigDecimal]("price", O.SqlType("DECIMAL(10,2)"))
  def totalPrice = column[BigDecimal]("total_price", O.SqlType("DECIMAL(10,2)"))

  // 表映射与OrderItem实体完全对齐
  override def * = (id, orderId, productId, quantity, price, totalPrice) <> (
    OrderItem.tupled, OrderItem.unapply
  )
}

object OrderItemTable { val query = TableQuery[OrderItemTable] }
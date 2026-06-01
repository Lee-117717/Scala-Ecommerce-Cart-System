package com.ecommerce.schema
import com.ecommerce.entity.CartItem
import slick.jdbc.MySQLProfile.api._

class CartTable(tag: Tag) extends Table[CartItem](tag, "cart_items") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def productId = column[String]("product_id")
  def quantity = column[Int]("quantity")
  def price = column[BigDecimal]("price", O.SqlType("DECIMAL(10,2)"))

  // 简化表映射（直接用tupled/unapply）
  override def * = (id, productId, quantity, price) <> (
    CartItem.tupled, CartItem.unapply
  )
}

object CartTable { val query = TableQuery[CartTable] }
package com.ecommerce.dao

import com.ecommerce.schema.OrderItemTable
import com.ecommerce.entity.OrderItem
import com.ecommerce.db.DBUtil
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration._

// 订单项查询对象
object OrderItemDAO {
  // 直接引用OrderItemTable的查询对象，避免重复定义
  val query: TableQuery[OrderItemTable] = OrderItemTable.query

  // 根据订单ID查订单项
  def getOrderItemsByOrderId(orderId: String): List[OrderItem] = {
    val action = query.filter(_.orderId === orderId).result
    Await.result(DBUtil.db.run(action), 5.seconds).toList
  }
}
package com.ecommerce.service
import com.ecommerce.dao.{CartDAO, OrderDAO, OrderItemDAO, ProductDAO}
import com.ecommerce.entity.{CartItem, Order, OrderItem, Product}
import com.ecommerce.db.DBUtil
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

object OrderService {
  def checkout(): Either[String, Order] = {
    val cartItems = CartService.listCartItems()
    if (cartItems.isEmpty) return Left("购物车为空！")

    // 加载商品信息
    val productMap = ProductService.listProducts().map(p => p.id -> p).toMap
    val missingProducts = cartItems.filterNot(ci => productMap.contains(ci.productId))
    if (missingProducts.nonEmpty) return Left(s"商品不存在：${missingProducts.map(_.productId).mkString(",")}")

    // 校验库存
    val stockShortage = cartItems.find(ci => productMap(ci.productId).quantity < ci.quantity)
    if (stockShortage.isDefined) {
      val product = productMap(stockShortage.get.productId)
      return Left(s"商品【${product.name}】库存不足！")
    }

    // 生成订单
    val orderId = UUID.randomUUID().toString
    val createTime = new Timestamp(System.currentTimeMillis())
    // 计算订单总金额（使用CartItem.price）
    val orderTotalAmount = cartItems.foldLeft(BigDecimal(0)) { (sum, ci) =>
      val itemTotal = ci.price * ci.quantity
      sum + itemTotal
    }
    val order = Order(orderId, orderTotalAmount, createTime)

    // 生成订单项（使用CartItem.price）
    val orderItems = cartItems.map { ci =>
      val itemTotal = ci.price * ci.quantity
      OrderItem(
        id = None,
        orderId = orderId,
        productId = ci.productId,
        quantity = ci.quantity,
        price = ci.price,
        totalPrice = itemTotal
      )
    }

    // 事务执行
    val transactionActions = DBIO.seq(
      OrderDAO.query += order,
      OrderItemDAO.query ++= orderItems,
      DBIO.sequence(cartItems.map(ci => ProductService.updateStock(ci.productId, ci.quantity))).map(_ => ()),
      CartDAO.query.delete
    )

    try {
      Await.result(DBUtil.db.run(transactionActions.transactionally.asTry), 10.seconds) match {
        case Success(_) => Right(order)
        case Failure(ex) => Left(s"结算失败：${ex.getMessage}")
      }
    } catch {
      case ex: Exception => Left(s"事务异常：${ex.getMessage}")
    }
  }

  def listOrders(): List[Order] = {
    val action = OrderDAO.query.result
    Await.result(DBUtil.db.run(action), 5.seconds).toList
  }

  def getOrderWithItems(orderId: String): Option[(Order, List[OrderItem])] = {
    val combinedAction = for {
      orderOpt <- OrderDAO.query.filter(_.id === orderId).result.headOption
      items <- OrderItemDAO.query.filter(_.orderId === orderId).result
    } yield (orderOpt, items)

    try {
      Await.result(DBUtil.db.run(combinedAction), 5.seconds) match {
        case (Some(order), items) => Some((order, items.toList)) // 转换为List
        case _ => None
      }
    } catch {
      case ex: Exception =>
        println(s"查询订单详情失败：${ex.getMessage}")
        None
    }
  }

  def updateOrderStatus(orderId: String, newStatus: String): Either[String, Unit] = {
    try {
      val updateAction = OrderDAO.query
        .filter(_.id === orderId)
        .map(_.status)
        .update(newStatus)

      // 执行更新操作
      val affectedRows = Await.result(DBUtil.db.run(updateAction), 5.seconds)

      if (affectedRows > 0) {
        Right(())
      } else {
        Left(s"订单$orderId 不存在，更新状态失败")
      }
    } catch {
      case ex: Exception => Left(s"更新订单状态失败：${ex.getMessage}")
    }
  }

}
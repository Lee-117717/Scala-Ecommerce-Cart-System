package com.ecommerce.service

import com.ecommerce.dao.CartDAO
import com.ecommerce.entity.CartItem
import com.ecommerce.db.DBUtil
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object CartService {
  /**
   * 1. 添加商品到购物车（对应需求：商品添加到购物车，先校验库存）
   * @param productId 商品ID
   * @param quantity 购买数量
   * @return Either[String, Unit]：Left(错误) / Right(成功)
   */
  def addToCart(productId: String, quantity: Int): Either[String, Unit] = {
    // 1.1 基础校验：数量必须大于0
    if (quantity <= 0) {
      return Left("购买数量必须大于1！")
    }

    // 1.2 先查商品是否存在、库存是否充足（调用ProductService，避免重复逻辑）
    ProductService.getProductById(productId) match {
      // 分支1：商品不存在，直接返回错误
      case None => Left("要添加的商品不存在！")

      // 分支2：商品存在但库存不足，直接返回错误（不执行任何数据库操作）
      case Some(product) if product.quantity < quantity =>
        Left(s"商品【${product.name}】库存不足！当前库存：${product.quantity}，请求数量：$quantity")

      // 分支3：商品存在且库存充足，执行购物车新增/更新操作
      case Some(product) =>
        // 数据库操作：购物车有该商品则更新数量，无则新增
        val dbAction = for {
          // 查购物车中是否已有该商品
          existItem <- CartDAO.query.filter(_.productId === productId).result.headOption
          _ <- existItem match {
            case Some(item) =>
              // 已有商品：更新数量（原数量 + 新数量）
              val newQuantity = item.quantity + quantity
              CartDAO.query
                .filter(_.productId === productId)
                .map(_.quantity)
                .update(newQuantity)
            case None =>
              // 无商品：新增购物车项（包含商品单价）
              CartDAO.query += CartItem(
                id = None,
                productId = productId,
                quantity = quantity,
                price = product.price // 补充商品单价
              )
          }
        } yield ()

        // 执行操作并返回结果
        try {
          Await.result(DBUtil.db.run(dbAction), 5.seconds)
          Right(())
        } catch {
          case ex: Exception => Left(s"添加购物车失败：${ex.getMessage}")
        }
    }
  }

  /**
   * 2. 修改购物车商品数量（对应需求：修改购物车商品，支持增加/减少/删除）
   * @param productId 商品ID
   * @param newQuantity 新数量（<=0则删除该商品）
   * @return Either[String, Unit]：Left(错误) / Right(成功)
   */
  def updateCartItem(productId: String, newQuantity: Int): Either[String, Unit] = {
    // 2.1 数量<=0：直接删除商品
    if (newQuantity <= 0) {
      return removeCartItem(productId)
    }

    // 2.2 数量>0：先校验库存，再更新
    ProductService.getProductById(productId) match {
      case None => Left("要修改的商品不存在！")
      case Some(product) if product.quantity < newQuantity =>
        Left(s"商品【${product.name}】库存不足！无法修改为${newQuantity}件")
      case Some(_) =>
        // 2.3 检查购物车是否有该商品
        val dbAction = for {
          exist <- CartDAO.query.filter(_.productId === productId).exists.result
          _ <- if (!exist) {
            DBIO.failed(new RuntimeException("购物车中无该商品！"))
          } else {
            CartDAO.query
              .filter(_.productId === productId)
              .map(_.quantity)
              .update(newQuantity)
          }
        } yield ()

        try {
          Await.result(DBUtil.db.run(dbAction), 5.seconds)
          Right(())
        } catch {
          case ex: Exception => Left(s"修改购物车失败：${ex.getMessage}")
        }
    }
  }

  /**
   * 3. 删除购物车商品（单个删除，对应需求：修改购物车的衍生功能）
   * @param productId 商品ID
   * @return Either[String, Unit]：Left(错误) / Right(成功)
   */
  def removeCartItem(productId: String): Either[String, Unit] = {
    val dbAction = for {
      exist <- CartDAO.query.filter(_.productId === productId).exists.result
      _ <- if (!exist) {
        DBIO.failed(new RuntimeException("购物车中无该商品，无需删除！"))
      } else {
        CartDAO.query.filter(_.productId === productId).delete // 执行删除
      }
    } yield ()

    try {
      Await.result(DBUtil.db.run(dbAction), 5.seconds)
      Right(())
    } catch {
      case ex: Exception => Left(s"删除购物车商品失败：${ex.getMessage}")
    }
  }

  /**
   * 4. 清空购物车（结算后自动调用，也支持手动清空）
   * @return Either[String, Unit]：Left(错误) / Right(成功)
   */
  def clearCart(): Either[String, Unit] = {
    val dbAction = CartDAO.query.delete // 删除所有购物车项
    try {
      Await.result(DBUtil.db.run(dbAction), 5.seconds)
      Right(())
    } catch {
      case ex: Exception => Left(s"清空购物车失败：${ex.getMessage}")
    }
  }

  /**
   * 5. 查看购物车列表（供Menu和OrderService调用）
   * @return List[CartItem]：购物车所有商品
   */
  def listCartItems(): List[CartItem] = {
    val dbAction = CartDAO.query.result
    try {
      Await.result(DBUtil.db.run(dbAction), 5.seconds).toList
    } catch {
      case ex: Exception =>
        println(s"查询购物车失败：${ex.getMessage}")
        List.empty
    }
  }
}
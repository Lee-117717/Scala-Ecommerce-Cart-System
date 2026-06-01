package com.ecommerce.service

import com.ecommerce.dao.ProductDAO
import com.ecommerce.entity.Product
import com.ecommerce.db.DBUtil
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ProductService {
  /**
   * 1. 新增商品（对应需求：创建库存文件，校验ID唯一、价格/库存非负）
   * @param product 商品实体（id/name/price/quantity）
   * @return Either[String, Unit]：Left(错误信息) / Right(成功)
   */
  def addProduct(product: Product): Either[String, Unit] = {
    // 1.1 基础参数校验
    if (product.id.trim.isEmpty || product.name.trim.isEmpty) {
      return Left("商品ID和名称不能为空！")
    }
    if (product.price < 0 || product.quantity < 0) {
      return Left("商品价格和库存不能为负数！")
    }

    // 1.2 数据库操作：先查ID是否已存在，再插入（避免重复）
    val dbAction = for {
      // 检查商品ID是否已存在
      exists <- ProductDAO.query.filter(_.id === product.id).exists.result
      _ <- if (exists) {
        DBIO.failed(new RuntimeException("商品ID已存在！")) // 触发失败，后续统一捕获
      } else {
        ProductDAO.query += product // 插入新商品
      }
    } yield ()

    // 1.3 执行数据库操作并处理结果
    try {
      Await.result(DBUtil.db.run(dbAction), 5.seconds)
      Right(()) // 成功返回
    } catch {
      case ex: Exception => Left(s"新增商品失败：${ex.getMessage}")
    }
  }

  /**
   * 2. 根据ID查询商品（供购物车、结算服务调用，返回具体商品信息）
   * @param productId 商品ID
   * @return Option[Product]：Some(商品) / None(商品不存在)
   */
  def getProductById(productId: String): Option[Product] = {
    val dbAction = ProductDAO.query.filter(_.id === productId).result.headOption
    try {
      Await.result(DBUtil.db.run(dbAction), 5.seconds)
    } catch {
      case ex: Exception =>
        println(s"查询商品失败：${ex.getMessage}")
        None
    }
  }

  /**
   * 3. 更新商品库存（供结算服务调用，事务内执行，返回Slick DBIO操作）
   * @param productId 商品ID
   * @param reduceQuantity 减少的库存数量（结算时的购买数量）
   * @return DBIO[Int]：影响的行数（1=成功，0=商品不存在）
   */
  def updateStock(productId: String, reduceQuantity: Int): DBIO[Int] = {
    // 先查当前库存，再计算新库存（避免并发超卖）
    ProductDAO.query.filter(_.id === productId).result.headOption.flatMap {
      case Some(product) =>
        val newStock = product.quantity - reduceQuantity
        if (newStock < 0) {
          DBIO.failed(new RuntimeException(s"商品【${product.name}】库存不足！"))
        } else {
          ProductDAO.query
            .filter(_.id === productId)
            .map(_.quantity)
            .update(newStock) // 更新库存
        }
      case None =>
        DBIO.failed(new RuntimeException(s"商品ID【$productId】不存在！"))
    }
  }

  /**
   * 4. 查看所有商品库存（对应需求：数据统计 - 查看库存商品信息）
   * @return List[Product]：所有商品列表（按ID升序，便于查看）
   */
  def listProducts(): List[Product] = {
    val dbAction = ProductDAO.query.sortBy(_.id.asc).result
    try {
      Await.result(DBUtil.db.run(dbAction), 5.seconds).toList
    } catch {
      case ex: Exception =>
        println(s"查询商品库存失败：${ex.getMessage}")
        List.empty
    }
  }
}
package com.ecommerce.dao

import com.ecommerce.schema.CartTable
import slick.jdbc.MySQLProfile.api._

// 购物车查询对象（业务层调用的核心入口）
object CartDAO {
  val query = TableQuery[CartTable]
}
package com.ecommerce.db

import slick.jdbc.MySQLProfile.api._

object DBUtil {
  // 手动创建数据库连接（绕过slick的配置加载）
  val db = Database.forURL(
    url = "jdbc:mysql://localhost:3306/ecommerce_cart?useSSL=false&characterEncoding=utf8&serverTimezone=UTC",
    user = "root",        // 数据库用户名
    password = "123456",  // 数据库密码
    driver = "com.mysql.jdbc.Driver"
  )
}
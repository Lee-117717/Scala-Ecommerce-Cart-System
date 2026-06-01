package com.ecommerce.dao

import com.ecommerce.schema.ProductTable

object ProductDAO {
  // 直接使用ProductTable伴生对象的query，避免重复定义
  val query = ProductTable.query
}
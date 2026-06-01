package com.ecommerce.entity

import java.sql.Timestamp

case class Order(
                  id: String,
                  totalAmount: BigDecimal,
                  createTime: Timestamp,
                  status: String = "待支付"
                )
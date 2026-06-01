package com.ecommerce.entity

case class OrderItem(
                      id: Option[Long],
                      orderId: String,
                      productId: String,
                      quantity: Int,
                      price: BigDecimal,
                      totalPrice: BigDecimal
                    )
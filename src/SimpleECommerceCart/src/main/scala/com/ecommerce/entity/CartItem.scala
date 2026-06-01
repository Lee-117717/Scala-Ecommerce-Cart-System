package com.ecommerce.entity

case class CartItem(
                     id: Option[Int],
                     productId: String,
                     quantity: Int,
                     price: BigDecimal
                   )
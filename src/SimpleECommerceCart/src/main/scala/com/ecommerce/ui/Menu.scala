package com.ecommerce.ui

import com.ecommerce.entity.{CartItem, Product, Order, OrderItem}
import com.ecommerce.service.{CartService, OrderService, ProductService}
import com.ecommerce.dao.OrderItemDAO
import scala.io.StdIn
import scala.util.Try
import java.sql.Timestamp

object Menu {
  implicit val timestampOrdering: Ordering[Timestamp] = Ordering.by(_.getTime)
  // 主菜单入口（程序启动后调用，初始化系统并展示菜单）
  def showMainMenu(): Unit = {
    println("===================== 欢迎使用李小燕电商购物车系统 =====================")
    println("\t\t📢 系统提示：所有操作数据将实时保存至数据库，确保交易安全可靠")
    while (true) { // 循环展示菜单，直至用户选择退出
      printMenu()
      // 捕获用户输入，避免非数字输入导致程序崩溃
      val input = StdIn.readLine().trim
      if (Try(input.toInt).isFailure) {
        println("❌ 输入错误！请输入1-8之间的数字选择操作\n")
      } else {
        handleInput(input.toInt) // 处理合法数字输入
      }
    }
  }

  // 1. 打印主菜单
  private def printMenu(): Unit = {
    println("\n========= 主菜单 ==========")
    println("1. 上架商品")
    println("2. 查看商品库存")
    println("3. 添加商品到购物车")
    println("4. 修改购物车商品数量")
    println("5. 查看购物车")
    println("6. 购物车结算")
    println("7. 查看所有订单")
    println("8. 退出系统")
    print("请选择操作（1-8）：")
  }

  // 2. 处理用户输入（分功能调用服务层，实现功能闭环）
  private def handleInput(choice: Int): Unit = choice match {
    case 1 => addProduct()        // 新增商品
    case 2 => listProducts()      // 查看库存
    case 3 => addToCart()         // 添加到购物车
    case 4 => updateCartItem()    // 修改购物车数量
    case 5 => listCartItems()     // 查看购物车
    case 6 => checkout()          // 购物车结算
    case 7 => listOrders()        // 查看订单
    case 8 => exitSystem()        // 退出系统
    case _ => println("❌ 输入错误！请选择1-8之间的数字\n")
  }

  /**
   * 功能1：新增商品（对应需求“创建库存文件”，校验商品ID唯一、价格/库存非负）
   * 适配：ProductService.addProduct()，确保输入合法后调用服务层
   */
  private def addProduct(): Unit = {
    println("\n========================= 上架商品 ===========================")
    // 商品ID校验（非空+避免重复）
    print("请输入商品ID（如prod_001）：")
    val id = StdIn.readLine().trim
    if (id.isEmpty) {
      println("❌ 商品ID不能为空！"); return
    }
    // 提前校验ID是否已存在，减少无效操作
    if (ProductService.getProductById(id).isDefined) {
      println(s"❌ 商品ID【$id】已存在，请重新输入！"); return
    }

    // 商品名称校验（非空）
    print("请输入上架商品名称：")
    val name = StdIn.readLine().trim
    if (name.isEmpty) {
      println("❌ 商品名称不能为空！"); return
    }

    // 商品价格校验（非负+数字格式）
    val price = Try {
      print("请输入商品单价（元，保留2位小数）：")
      val input = StdIn.readLine().trim
      if (input.isEmpty) throw new RuntimeException("价格不能为空！")
      val p = BigDecimal(input)
      if (p < 0) throw new RuntimeException("价格不能为负")
      // 强制保留2位小数，确保金额精度（避免0.10显示为0.1）
      p.setScale(2, BigDecimal.RoundingMode.HALF_UP)
    }.recover {
      case ex: Exception =>
        println(s"⚠️ 价格输入错误：${ex.getMessage}（示例正确输入：99.99）")
        BigDecimal(-1) // 标记为无效值
    }.getOrElse(BigDecimal(-1))

    // 商品库存校验（非负+整数格式）
    val quantity = Try {
      print("请输入上架商品库存（件，非负整数）：")
      val input = StdIn.readLine().trim
      if (input.isEmpty) throw new RuntimeException("库存不能为空！")
      val q = input.toInt
      if (q < 0) throw new RuntimeException("库存不能为负数！")
      q
    }.recover {
      case ex: Exception =>
        println(s"⚠️ 库存输入错误：${ex.getMessage}（示例正确输入：100）")
        -1 // 标记为无效值
    }.getOrElse(-1)

    // 调用服务层新增商品（仅当输入全部有效时）
    if (price >= 0 && quantity >= 0) {
      ProductService.addProduct(Product(id, name, price, quantity)) match {
        case Right(_) =>
          println(s"✅ 商品上架成功！")
          println(s"📦 商品信息：ID=$id，名称=$name，单价=$price 元，库存=$quantity 件")
        case Left(msg) => println(s"❌ 商品新增失败：$msg")
      }
    } else {
      println("❌ 商品上架失败，请重新输入合法数据！")
    }
  }

  /**
   * 功能2：查看商品库存（对应需求“数据统计-查看库存”，新增库存预警提示）
   * 适配：ProductService.listProducts()，格式化输出并引导用户操作
   */
  private def listProducts(): Unit = {
    println("\n=========================================== 商品库存列表 ===========================================")
    val products = ProductService.listProducts()
    if (products.isEmpty) {
      println("⚠️ 当前无商品库存！")
      print("是否立即上架商品？（y/n，默认n）：")
      val choice = StdIn.readLine().trim.toLowerCase
      if (choice == "y") addProduct()
      return
    }

    // 格式化输出（含库存状态预警，避免用户添加无库存商品）
    println(f"${"序号"}%5s | ${"商品ID"}%12s | ${"商品名称"}%15s | ${"单价(元)"}%12s | ${"库存(件)"}%12s | ${"状态"}%8s")
    println("-" * 98)
    products.zipWithIndex.foreach { case (p, idx) =>
      val stockStatus = p.quantity match {
        case q if q <= 0 => "❌ 已售罄"
        case q if q <= 5 => "⚠️ 库存紧张"
        case _ => "✅ 充足"
      }
      println(f"${idx + 1}%5d | ${p.id}%12s | ${p.name}%15s | ${p.price}%12.2f | ${p.quantity}%12d | $stockStatus%8s")
    }

    // 引导用户添加商品到购物车
    print("\n是否选择商品添加到购物车？（输入商品序号，0返回主菜单）：")
    val seqInput = Try(StdIn.readInt()).getOrElse(-1)
    if (seqInput > 0 && seqInput <= products.size) {
      val selectedProduct = products(seqInput - 1)
      // 直接跳转添加购物车，自动填充商品ID
      addToCart(selectedProduct.id)
    } else if (seqInput != 0) {
      println("❌ 输入序号无效，返回主菜单！")
    }
  }

  /**
   * 功能3：添加商品到购物车（对应需求“商品添加到购物车”，校验库存后添加）
   * 适配：CartService.addToCart()，支持手动输入ID和自动填充ID两种场景
   */
  private def addToCart(): Unit = addToCart("")
  private def addToCart(defaultProductId: String): Unit = {
    println("\n===================== 添加商品到购物车 =====================")
    // 商品ID处理（支持自动填充，减少用户输入）
    val productId = if (defaultProductId.nonEmpty) {
      println(s"自动填充商品ID：$defaultProductId")
      defaultProductId
    } else {
      print("请输入商品ID：")
      StdIn.readLine().trim
    }
    if (productId.isEmpty) {
      println("❌ 商品ID不能为空！"); return
    }

    // 购买数量校验（≥1，且不超过库存）
    val quantity = Try {
      print("请输入购买数量（≥1）：")
      val input = StdIn.readLine().trim
      if (input.isEmpty) throw new RuntimeException("数量不能为空！")
      val q = input.toInt
      if (q < 1) throw new RuntimeException("数量必须≥1！")
      q
    }.recover {
      case ex: Exception =>
        println(s"⚠️ 数量输入错误：${ex.getMessage}")
        -1
    }.getOrElse(-1)

    if (quantity < 1) {
      println("❌ 购买数量无效，添加失败！")
      return
    }

    // 调用服务层添加商品，处理结果
    CartService.addToCart(productId, quantity) match {
      case Right(_) =>
        val product = ProductService.getProductById(productId)
        product.foreach(p => println(s"✅ 商品添加成功！商品：${p.name}，数量：$quantity 件，单价：${p.price}元"))
      case Left(msg) => println(s"❌ 添加失败：$msg")
    }
  }

  /**
   * 功能4：修改购物车商品数量（对应需求“修改购物车商品”，支持删除商品）
   * 适配：CartService.updateCartItem()，数量≤0时自动删除
   */
  private def updateCartItem(): Unit = {
    println("\n============== 修改购物车商品数量 ==============")
    // 先展示当前购物车，避免用户忘记商品ID
    val cartItems = CartService.listCartItems()
    if (cartItems.isEmpty) {
      println("⚠️ 您的购物车为空，无需修改！"); return
    }
    println("当前购物车商品：")
    cartItems.foreach { item =>
      ProductService.getProductById(item.productId).foreach { p =>
        println(s"- 商品ID：${p.id}，名称：${p.name}，当前数量：${item.quantity}件")
      }
    }

    // 输入商品ID和新数量
    print("\n请输入要修改的商品ID：")
    val productId = StdIn.readLine().trim
    val newQuantity = Try {
      print("请输入新的数量（0=删除该商品，≥1=修改数量）：")
      StdIn.readInt()
    }.getOrElse(-1)

    // 校验数量合法性
    if (newQuantity < 0) {
      println("❌ 数量不能为负！"); return
    }

    // 调用服务层修改，处理结果
    CartService.updateCartItem(productId, newQuantity) match {
      case Right(_) =>
        if (newQuantity <= 0) {
          println(s"✅ 商品【$productId】已从购物车删除！")
        } else {
          ProductService.getProductById(productId).foreach { p =>
            println(s"✅ 商品【${p.name}】数量已修改为${newQuantity}件！")
          }
        }
      case Left(msg) => println(s"❌ 修改失败：$msg")
    }
  }

  /**
   * 功能5：查看购物车（对应需求“查看购物车商品”，计算总计并引导结算）
   * 适配：CartService.listCartItems()，展示商品详情、小计和总计
   */
  private def listCartItems(): Unit = {
    println("\n====================================== 购物车列表 ======================================")
    val cartItems = CartService.listCartItems()
    if (cartItems.isEmpty) {
      println("⚠️ 您的购物车为空！")
      print("是否立即添加商品？（y/n，默认n）：")
      val choice = StdIn.readLine().trim.toLowerCase
      if (choice == "y") addToCart()
      return
    }

    // 格式化输出（含商品名称、单价、小计，提升用户体验）
    println(f"${"商品ID"}%12s | ${"商品名称"}%15s | ${"单价(元)"}%12s | ${"数量(件)"}%12s | ${"小计(元)"}%12s")
    println("-" * 87)
    var total = BigDecimal(0.0).setScale(2, BigDecimal.RoundingMode.HALF_UP)
    cartItems.foreach { item =>
      ProductService.getProductById(item.productId).foreach { p =>
        val subtotal = (p.price * item.quantity).setScale(2, BigDecimal.RoundingMode.HALF_UP)
        total += subtotal
        println(f"${p.id}%12s | ${p.name}%15s | ${p.price}%12.2f | ${item.quantity}%12d | $subtotal%12.2f")
      }
    }
    println("-" * 87)
    println(f"${"购物车总计"}%52s | $total%12.2f元")

    // 引导用户结算（减少操作步骤）
    print("\n是否立即结算当前购物车？（y/n，默认n）：")
    val checkoutChoice = StdIn.readLine().trim.toLowerCase
    if (checkoutChoice == "y") checkout()
  }

  /**
   * 功能6：购物车结算（对应需求“购物车结算”，生成订单并展示订单项）
   * 适配：OrderService.checkout()，与OrderItemDAO联动展示详情
   */
  private def checkout(): Unit = {
    println("\n======================== 购物车结算 ==========================")
    println("\t\t📢 结算提示：系统将校验库存并锁定商品，结算成功后扣减库存")

    OrderService.checkout() match {
      case Right(order) =>
        println("\n✅ 结算成功！生成订单如下：")
        println("-" * 50)
        println(f"订单ID：${order.id}")
        println(f"创建时间：${order.createTime}")
        println(f"订单状态：${order.status}")
        println(f"订单总金额：${order.totalAmount.setScale(2, BigDecimal.RoundingMode.HALF_UP)}元")
        println("=" * 50)

        // 展示订单项详情
        println("\n📦 订单项详情：")
        println("=" * 88)
        val orderItems = OrderItemDAO.getOrderItemsByOrderId(order.id)
        println(f"${"商品ID"}%12s | ${"商品名称"}%15s | ${"单价(元)"}%12s | ${"数量(件)"}%12s | ${"小计(元)"}%12s")
        println("-" * 88)
        orderItems.foreach { item =>
          ProductService.getProductById(item.productId).foreach { p =>
            println(f"${p.id}%12s | ${p.name}%15s | ${item.price}%12.2f | ${item.quantity}%12d | ${item.totalPrice}%12.2f")
          }
        }
        println("-" * 88)
        println(f"${"订单总计"}%52s | ${order.totalAmount}%12.2f元")
        println("\n💡 提示：可通过「查看所有订单」功能查看历史订单详情")

        // 新增：模拟支付交互
        print("\n是否立即支付该订单？(y/n，默认n)：")
        val payChoice = StdIn.readLine().trim.toLowerCase
        if (payChoice == "y") {
          val payResult = OrderService.updateOrderStatus(order.id, "已支付")
          payResult match {
            case Right(_) =>
              println(s"✅ 订单${order.id}支付成功！状态已更新为【已支付】")
            case Left(error) => println(s"❌ 支付失败：$error")
          }
        } else {
          println(s"ℹ️ 你选择了暂不支付，订单${order.id}状态仍为【${order.status}】")
        }

      case Left(msg) =>
        println(s"❌ 结算失败：$msg")
        print("是否返回购物车修改商品？（y/n，默认n）：")
        val choice = StdIn.readLine().trim.toLowerCase
        if (choice == "y") listCartItems()
    }
  }

  /**
   * 功能7：查看所有订单（对应需求“数据统计-查看订单”，支持查看单个订单详情）
   * 适配：OrderService.listOrders()，按时间倒序展示并支持详情查询
   */
  private def listOrders(): Unit = {
    println("\n=========================================== 所有订单列表 ===========================================")
    val orders = OrderService.listOrders()
    if (orders.isEmpty) {
      println("⚠️ 您暂无订单记录！")
      print("是否立即结算购物车生成订单？（y/n，默认n）：")
      val choice = StdIn.readLine().trim.toLowerCase
      if (choice == "y") checkout()
      return
    }


    // 订单列表（按创建时间倒序，最新订单在前）
    println(f"${"序号"}%5s | ${"订单ID"}%35s | ${"创建时间"}%22s | ${"订单金额(元)"}%15s | ${"订单状态"}%10s")
    println("-" * 110) // 调整分隔线长度

    val sortedOrders = orders.sortBy(_.createTime).reverse
    sortedOrders.zipWithIndex.foreach { case (order, idx) =>
      // 展示订单状态
      println(f"${idx + 1}%5d | ${order.id}%35s | ${order.createTime}%22s | ${order.totalAmount}%15.2f | ${order.status}%10s")
    }

    // 查看单个订单详情
    print("\n请输入要查看的订单序号（0返回主菜单）：")
    val seqInput = Try(StdIn.readInt()).getOrElse(-1)
    if (seqInput > 0 && seqInput <= orders.size) {
      val selectedOrder = sortedOrders(seqInput - 1)
      println(s"\n========================= 订单详情 ==========================")
      println(f"订单ID：${selectedOrder.id}")
      println(f"创建时间：${selectedOrder.createTime}")
      println(f"订单状态：${selectedOrder.status}") // 新增：展示订单状态
      println(f"订单总金额：${selectedOrder.totalAmount}%.2f元")
      println("-" * 60)

      // 订单项详情
      val orderItems = OrderItemDAO.getOrderItemsByOrderId(selectedOrder.id)
      println("订单项：")
      orderItems.foreach { item =>
        ProductService.getProductById(item.productId).foreach { p =>
          println(s"- ${p.name}：${item.quantity}件 × ${item.price}元/件 = ${item.totalPrice}元")
        }
      }
      println("=" * 60)

      if (selectedOrder.status == "待支付") {
        print("\n该订单为【待支付】状态，是否立即支付？(y/n，默认n)：")
        val payChoice = StdIn.readLine().trim.toLowerCase
        if (payChoice == "y") {
          val payResult = OrderService.updateOrderStatus(selectedOrder.id, "已支付")
          payResult match {
            case Right(_) =>
              println(s"✅ 订单${selectedOrder.id}支付成功！状态已更新为【已支付】")
              println("\n========================= 刷新后订单列表 ==========================")
              listOrders() // 递归调用自身，刷新列表
            case Left(error) => println(s"❌ 支付失败：$error")
          }
        }
      }

    } else if (seqInput != 0) {
      println("❌ 输入序号无效，返回主菜单！")
    }
  }

  /**
   * 功能8：退出系统（对应需求“退出系统”，确保数据保存并友好提示）
   * 适配：数据库自动持久化，无需额外操作，正常退出程序
   */
  private def exitSystem(): Unit = {
    println("\n========================== 退出系统 ==============================")
    println("\t\t📢 系统提示：所有数据已自动保存至数据库，请放心退出！")
    println("\t\t\t感谢使用李小燕电商购物车系统，祝您生活愉快！")
    System.exit(0) // 正常终止程序，释放资源
  }
}
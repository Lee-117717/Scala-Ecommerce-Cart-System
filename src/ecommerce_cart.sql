-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS ecommerce_cart;

USE ecommerce_cart; -- 切换到目标数据库

-- 2. 商品表（products）：存储商品库存信息，对应需求“创建库存文件”
CREATE TABLE products (
  id VARCHAR(50) PRIMARY KEY COMMENT '商品ID',
  name VARCHAR(100) NOT NULL COMMENT '商品名称）',
  price DECIMAL(10,2) NOT NULL COMMENT '商品单价（保留2位小数）',
  quantity INT NOT NULL COMMENT '商品库存数量（≥0）',
  UNIQUE KEY uk_product_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';

-- 3. 订单表（orders）：存储订单核心信息，对应需求“购物车结算生成订单”
CREATE TABLE orders (
  id VARCHAR(50) PRIMARY KEY COMMENT '订单ID',
  total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间',
  status VARCHAR(20) NOT NULL DEFAULT '待支付' COMMENT '订单状态（待支付/已支付/已取消/已完成）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 4. 订单项表（order_items）：存储订单中的单个商品记录，补充 price 和 total_price 字段
CREATE TABLE order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单项自增ID',
  order_id VARCHAR(50) NOT NULL COMMENT '关联的订单ID',
  product_id VARCHAR(50) NOT NULL COMMENT '关联的商品ID',
  quantity INT NOT NULL COMMENT '购买数量（≥1）',
  price DECIMAL(10,2) NOT NULL COMMENT '商品购买时的单价',
  total_price DECIMAL(10,2) NOT NULL COMMENT '订单项小计',
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项明细表';

-- 5. 购物车表（cart_items）：存储用户购物车数据，新增 price 字段记录加入时单价
CREATE TABLE cart_items (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车项自增ID',
  product_id VARCHAR(50) NOT NULL COMMENT '关联的商品ID',
  quantity INT NOT NULL COMMENT '购物车中商品数量（≥1）',
  price DECIMAL(10,2) NOT NULL COMMENT '商品加入购物车时的单价（固定当时价格）',
  add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '商品加入购物车的时间（最新的添加到置顶）',
  UNIQUE KEY uk_cart_product (product_id),
  INDEX idx_cart_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';



ALTER TABLE order_items 
ADD CONSTRAINT order_item_order_fk 
FOREIGN KEY (order_id) REFERENCES orders(id) 
ON DELETE CASCADE;

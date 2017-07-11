package com.example.w

import scala.collection.immutable

/**
  * Created by Diem on 11.07.2017.
  */
case class Client(id: String, balances: immutable.Map[String, Int])

case class Order(client: String,
                 isBuy: Boolean,
                 security: String,
                 price: Int,
                 quantity: Int,
                 timestamp: Long)

case class NewOrder(client: String,
                    isBuy: Boolean,
                    security: String,
                    price: Int,
                    quantity: Int)

case class UpdateBalance(client1: String,
                         client2: String,
                         isBuy: Boolean,
                         security: String,
                         price: Int,
                         quantity: Int)

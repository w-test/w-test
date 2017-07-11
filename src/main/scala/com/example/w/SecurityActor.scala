package com.example.w

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.example.w.ExchangeManagerActor.{GetBuyOrders, GetSellOrders}

import scala.annotation.tailrec
import scala.collection.immutable

/**
  * Created by Diem on 11.07.2017.
  */
class SecurityActor(id: String, manager: ActorRef)
    extends Actor
    with ActorLogging {
  import SecurityActor._

  private var buyOrders = immutable.SortedSet.empty[Order](buyOrdering)
  private var sellOrders = immutable.SortedSet.empty[Order](sellOrdering)

  override def receive: Receive = {
    case o: NewOrder =>
      val time = System.nanoTime()
      val order =
        Order(o.client, o.isBuy, o.security, o.price, o.quantity, time)
      processOrder(order)
      sender() ! order
//      log.info(s"Buy Orders: $buyOrders")
//      log.info(s"Sell Orders: $sellOrders\n")

    case b: GetBuyOrders =>
      sender() ! buyOrders

    case s: GetSellOrders =>
      sender() ! sellOrders

    case _ =>
  }

  @tailrec
  private def processOrder(order: Order): Unit = {
    def updateBalances(order: Order, oppositeOrder: Order) = {
      val qty = Math.min(order.quantity, oppositeOrder.quantity)
      val price = Math.min(order.price, oppositeOrder.price)
      manager ! UpdateBalance(order.client,
                              oppositeOrder.client,
                              order.isBuy,
                              order.security,
                              price,
                              qty)
    }

    if (order.isBuy) {
      if (sellOrders.isEmpty) {
        buyOrders = buyOrders + order
      } else {
        val oppositeOrder = sellOrders.head
        if (oppositeOrder.price <= order.price) {
          sellOrders = sellOrders.drop(1)
          if (order.quantity <= oppositeOrder.quantity) {
            val qty = oppositeOrder.quantity - order.quantity
            if (qty > 0) {
              sellOrders = sellOrders + oppositeOrder.copy(quantity = qty)
            }
            updateBalances(order, oppositeOrder)
          } else {
            val qty = order.quantity - oppositeOrder.quantity
            updateBalances(order, oppositeOrder)
            processOrder(order.copy(quantity = qty))
          }
        } else {
          buyOrders = buyOrders + order
        }
      }
    } else {
      if (buyOrders.isEmpty) {
        sellOrders = sellOrders + order
      } else {
        val oppositeOrder = buyOrders.head
        if (oppositeOrder.price >= order.price) {
          buyOrders = buyOrders.drop(1)
          if (order.quantity <= oppositeOrder.quantity) {
            val qty = oppositeOrder.quantity - order.quantity
            if (qty > 0) {
              buyOrders = buyOrders + oppositeOrder.copy(quantity = qty)
            }
            updateBalances(order, oppositeOrder)
          } else {
            val qty = order.quantity - oppositeOrder.quantity
            updateBalances(order, oppositeOrder)
            processOrder(order.copy(quantity = qty))
          }
        } else {
          sellOrders = sellOrders + order
        }
      }
    }
  }
}

object SecurityActor {
  val buyOrdering: Ordering[Order] = Ordering.by { order: Order =>
    (-order.price, -order.timestamp)
  }
  val sellOrdering: Ordering[Order] = Ordering.by { order: Order =>
    (order.price, -order.timestamp)
  }

  def props(security: String, manager: ActorRef): Props =
    Props(new SecurityActor(security, manager))
}

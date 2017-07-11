package com.example.w

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.immutable

/**
  * Created by Diem on 11.07.2017.
  */
class ExchangeManagerActor(scs: Seq[String],
                           cts: immutable.Map[String, Client])
    extends Actor
    with ActorLogging {
  import ExchangeManagerActor._

  var securities = immutable.Map.empty[String, ActorRef]
  var clients = immutable.Map.empty[String, Client]

  override def receive: Receive = {
    case newOrder: NewOrder =>
      val actor = securities.get(newOrder.security)
      if (actor.isDefined)
        actor.get forward newOrder

    case upd: UpdateBalance =>
      val client1 = clients(upd.client1)
      val client2 = clients(upd.client2)

      val USD1 =
        if (upd.isBuy) client1.balances("USD") - upd.quantity * upd.price
        else client1.balances("USD") + upd.quantity * upd.price

      val USD2 =
        if (upd.isBuy) client2.balances("USD") + upd.quantity * upd.price
        else client2.balances("USD") - upd.quantity * upd.price

      val security1 =
        if (upd.isBuy) client1.balances(upd.security) + upd.quantity
        else
          client1.balances(upd.security) - upd.quantity

      val security2 =
        if (upd.isBuy) client2.balances(upd.security) - upd.quantity
        else
          client2.balances(upd.security) + upd.quantity

      val c1balances = client1.balances
        .updated("USD", USD1)
        .updated(upd.security, security1)

      val c2balances = client2.balances
        .updated("USD", USD2)
        .updated(upd.security, security2)

      clients = clients
        .updated(client1.id, Client(client1.id, c1balances))
        .updated(client2.id, Client(client2.id, c2balances))

//      log.info(s"Balances: ${clients("C2")}\n")

    case GetBalances =>
      sender() ! clients.values.toVector.sortWith((c1, c2) => c1.id < c2.id)

    case b: GetBuyOrders =>
      val actor = securities.get(b.id)
      if (actor.isDefined)
        actor.get forward b

    case s: GetSellOrders =>
      val actor = securities.get(s.id)
      if (actor.isDefined)
        actor.get forward s

    case _ =>
  }

  override def preStart(): Unit = {
    initSecurities(scs)
    clients = cts
  }

  private def initSecurities(xs: Seq[String]): Unit = {
    xs foreach { x =>
      val secActor = context.actorOf(SecurityActor.props(x, self), x)
      securities = securities + (x -> secActor)
    }
  }
}

object ExchangeManagerActor {
  case object GetBalances
  case class GetBuyOrders(id: String)
  case class GetSellOrders(id: String)

  def props(scs: Seq[String], cts: immutable.Map[String, Client]): Props =
    Props(new ExchangeManagerActor(scs, cts))
}

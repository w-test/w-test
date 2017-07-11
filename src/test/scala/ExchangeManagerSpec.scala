import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.example.w._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.immutable

/**
  * Created by w-test on 11.07.2017.
  */
class ExchangeManagerSpec
    extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Exchange Manager Actor" must {

    val balances = immutable.Map[String, Int](
      "USD" -> 1000,
      "A" -> 100,
      "B" -> 100,
      "C" -> 100,
      "D" -> 100
    )

    val client1 = Client("C1", balances)
    val client2 = Client("C2", balances)
    val client8 = Client("C8", balances)
    val client9 = Client("C9", balances)

    val securities = List("A", "B", "C", "D")

    val clients = immutable.Map[String, Client](
      client1.id -> client1,
      client2.id -> client2,
      client8.id -> client8,
      client9.id -> client9
    )

    val manager = system.actorOf(
      ExchangeManagerActor.props(securities, clients),
      "exchange-manager-actor-test"
    )

    val order1 = NewOrder("C8", true, "C", 15, 4)
    val order2 = NewOrder("C2", false, "C", 14, 5)
    val order3 = NewOrder("C2", false, "C", 13, 2)
    val order4 = NewOrder("C9", true, "C", 14, 5)
    val order5 = NewOrder("C2", false, "C", 14, 2)
    val order6 = NewOrder("C1", false, "C", 15, 3)
    val order7 = NewOrder("C2", true, "C", 15, 1)
    val order8 = NewOrder("C2", false, "C", 13, 1)

    "accept new buy orders" in {
      manager ! order1
      val ord = expectMsgType[Order]
      ord.client shouldEqual order1.client
      ord.isBuy shouldEqual order1.isBuy
      ord.price shouldEqual order1.price
      ord.security shouldEqual order1.security
      ord.quantity shouldEqual order1.quantity

      manager ! ExchangeManagerActor.GetBuyOrders("C")
      expectMsg(immutable.SortedSet(ord)(SecurityActor.buyOrdering))
    }

    "accept new sell orders" in {
      manager ! order2
      val ord = expectMsgType[Order]
      ord.client shouldEqual order2.client
      ord.isBuy shouldEqual order2.isBuy
      ord.price shouldEqual order2.price
      ord.security shouldEqual order2.security
      ord.quantity shouldEqual order2.quantity

      manager ! ExchangeManagerActor.GetSellOrders("C")
      expectMsg(immutable.SortedSet(ord)(SecurityActor.sellOrdering))
    }

    "clients balances should be correct" in {
      manager ! ExchangeManagerActor.GetBalances
      val b2 = balances.updated("USD", 1000 + 4 * 14).updated("C", 100 - 4)
      val b8 = balances.updated("USD", 1000 - 4 * 14).updated("C", 100 + 4)
      expectMsg(
        Vector(client1,
               client2.copy(balances = b2),
               client8.copy(balances = b8),
               client9))
    }

    "matching should work 1" in {
      manager ! order3
      expectMsgType[Order]
      manager ! ExchangeManagerActor.GetSellOrders("C")
      val set = expectMsgType[immutable.SortedSet[Order]]
      set.head.security shouldEqual "C"
      set.head.price shouldEqual 13
      set.head.quantity shouldEqual 2
      set.size shouldEqual 2
    }

    "matching should work 2" in {
      manager ! order4
      expectMsgType[Order]
      manager ! ExchangeManagerActor.GetBuyOrders("C")
      val set = expectMsgType[immutable.SortedSet[Order]]
      set.head.security shouldEqual "C"
      set.head.price shouldEqual 14
      set.head.quantity shouldEqual 2
      set.head.client shouldEqual "C9"
      set.size shouldEqual 1
    }
  }
}

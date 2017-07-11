package com.example.w

import java.io.{File, PrintWriter}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.example.w.ExchangeManagerActor.GetBalances

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
  * Created by Diem on 11.07.2017.
  */
object Main {
  implicit val system = ActorSystem("system")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  private val securities = List("A", "B", "C", "D")
  private val clients = parseClients("/clients.txt")

  private val managerActor =
    system.actorOf(ExchangeManagerActor.props(securities, clients),
                   "exchange-manager-actor")

  def main(args: Array[String]): Unit = {
    parseOrders("/orders.txt")

    (managerActor ? GetBalances).mapTo[Vector[Client]] foreach { clients =>
      //clients.foreach(println)
      dumpResults(clients, "result.txt")
      system.terminate()
    }
  }

  private def dumpResults(clients: Seq[Client], filename: String): Unit = {
    using(new PrintWriter(new File(filename), "UTF-8")) { pw =>
      clients foreach { client =>
        val tsv = s"${client.id}\t${client.balances("USD")}\t" +
          s"${client.balances("A")}\t${client.balances("B")}\t" +
          s"${client.balances("C")}\t${client.balances("D")}\r\n"
        pw.write(tsv)
      }
    }
  }

  private def parseOrders(resourceName: String): Unit = {
    val source = io.Source.fromFile(getClass.getResource(resourceName).toURI)

    using(source) { s =>
      s.getLines foreach { line =>
        val fields = line.split("\t").map(_.trim)

        val order =
          NewOrder(fields(0),
                   if (fields(1).compareToIgnoreCase("b") == 0) true
                   else false,
                   fields(2),
                   fields(3).toInt,
                   fields(4).toInt)

        // pitfall:
        // not closing the Source will leave the file open
        // using bang instead of ask pattern here would result in early stream collapse and dead letters
        // doing some sort of order confirmation prevents Source from closing and ensures that all messages were sent
        (managerActor ? order).mapTo[Order]
      }
    }
  }

  private def parseClients(resourceName: String) = {
    val source = io.Source.fromFile(getClass.getResource(resourceName).toURI)
    var clients = immutable.Map.empty[String, Client]

    using(source) { s =>
      s.getLines foreach { line =>
        val fields = line.split("\t").map(_.trim)
        val balances = immutable.Map[String, Int](
          "USD" -> fields(1).toInt,
          "A" -> fields(2).toInt,
          "B" -> fields(3).toInt,
          "C" -> fields(4).toInt,
          "D" -> fields(5).toInt
        )
        clients = clients + (fields(0) -> Client(fields(0), balances))
      }
    }

    clients
  }
}

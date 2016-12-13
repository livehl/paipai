package main

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.routing.RoundRobinPool
import common.Tool._
import common.{Aliyun, OtsCache}
import db._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by isaac on 16/4/15.
  */
object DataSave {
  var dealCount=new AtomicInteger(0)

  def main(array: Array[String]): Unit = {
    val allCount = new LoanText().queryCount("")
    println(allCount)
    val very = array.headOption.map(_ == "true").getOrElse(false)
    println(very)
    val system = ActorSystem("PaiPai")
    var addCount=0
    val updateActor = system.actorOf(Props(new UpdateActor()), "updateActor")
    val fileActor = system.actorOf(Props(new FileActor(updateActor)).withRouter(new RoundRobinPool(4)), "fileActor")
    val tableActor = system.actorOf(Props(new TableActor(updateActor)), "tableActor")

    0 to (allCount / 100 + 1) foreach { page =>
      while(addCount - 1000 > dealCount.get()){
        println("wait:"+(addCount - dealCount.get))
        Thread.sleep(3000)
      }
      println(page)
      val loans=DBEntity.queryPage(new LoanText().getClass,"select lt.id,lt.createTime,l.Title,l.ListingId from LoanText lt ,Loan l where lt.id=l.id order by lt.createTime asc",page,100)
      if(!loans.isEmpty) {
        val caches = OtsCache.getCaches(loans.map(_.ListingId.toString): _*)
        val noCache = new LoanText().queryByIds(loans.filter(l => !caches.contains(l.ListingId)).map(_.id.toString)).map(l => l.ListingId -> l.text).toMap
        loans.foreach { loan =>
          val fullData = if (caches.get(loan.ListingId).isDefined) caches.get(loan.ListingId) else noCache.get(loan.ListingId)
          if (fullData.isDefined && fullData.get.length > 100) {
            val newLoan = new LoanText(loan.id, loan.Title, loan.ListingId, fullData.get, loan.createTime)
            tableActor ! newLoan
            fileActor ! newLoan
          } else {
            updateActor ! loan
            updateActor ! loan
          }
          addCount += 1
        }
      }
    }
    tableActor ! Clean
    while(addCount > dealCount.get){
      println("wait:"+(addCount - dealCount.get))
      Thread.sleep(3000)
    }
    System.exit(0)

  }
}


/**
  * 消息接收actor
  */
class FileActor(update:ActorRef) extends Actor {
  def receive = {
    case loan:LoanText  =>
      safe {
        Aliyun.saveFile("loan/" + loan.id, loan.text.getBytes("utf-8"))
        update ! loan
      }
  }
}

class TableActor(update:ActorRef) extends Actor {
  val list=ArrayBuffer[LoanText]()
  def receive = {
    case  loan:LoanText =>
      list.append(loan)
      if(list.size>=20){
        safe {
          BDBEntity.insertBatch(list.map(l => new LoanData(l.ListingId.toString, l.Title, l.text, l.createTime)).toList)
          list.foreach(l => update ! l)
          list.clear()
        }
      }
    case Clean=>
      if(list.size>0) {
        safe {
          BDBEntity.insertBatch(list.map(l => new LoanData(l.ListingId.toString, l.Title, l.text, l.createTime)).toList)
          list.foreach(l => update ! l)
          list.clear()
        }
      }
      update ! Clean
  }
}


class UpdateActor() extends Actor {
  val idSet=new mutable.HashSet[String]()
  val list=ArrayBuffer[String]()
  def receive = {
    case  loan:LoanText =>
      if(idSet.contains(loan.ListingId)){
        list.append(loan.id)
        if(list.size>=20){
          safe {
            OtsCache.delCaches(list.toList)
            DBEntity.sql(s"delete from loantext where  id in (${list.mkString(",")})")
            list.clear()
            DataSave.dealCount.addAndGet(list.size)
          }
        }
        idSet.remove(loan.ListingId)
      }else{
        idSet.add(loan.ListingId)
      }
    case Clean=>
      if(list.size>0) {
        safe {
            OtsCache.delCaches(list.toList)
            DBEntity.sql(s"delete from loantext where  id in (${list.mkString(",")})")
            list.clear()
            DataSave.dealCount.addAndGet(list.size)
          }
      }
  }
}

case class Clean()

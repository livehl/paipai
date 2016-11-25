package main

import akka.actor._
import common.Tool._
import common.{Aliyun, OtsCache}
import db._

import scala.collection.mutable

/**
  * Created by isaac on 16/4/15.
  */
object DataSave {
  var dealCount=0

  def main(array: Array[String]): Unit = {
    val allCount = new LoanText().queryCount("")
    println(allCount)
    val very = array.headOption.map(_ == "true").getOrElse(false)
    println(very)
    val system = ActorSystem("PaiPai")
    var addCount=0
    val updateActor = system.actorOf(Props(new UpdateActor()), "updateActor")
    val fileActor = system.actorOf(Props(new FileActor(updateActor)), "fileActor")
    val tableActor = system.actorOf(Props(new TableActor(updateActor)), "tableActor")

    1 to allCount / 100 foreach { page =>
      println(page)
      new LoanText().queryPage("", page, 100, "createTime asc")._2.map { loan =>
        if (loan.text.length > 100) {
          tableActor ! loan
          fileActor ! loan
          addCount+=1
        }
      }
    }
    if(addCount > dealCount){
      println("wait:"+(addCount - dealCount))
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
      Aliyun.saveFile("loan/" + loan.id, loan.text.getBytes("utf-8"))
      update ! loan
  }
}

class TableActor(update:ActorRef) extends Actor {
  def receive = {
    case  loan:LoanText =>
      new LoanData(loan.ListingId.toString, loan.Title, loan.text, loan.createTime).insert()
      update ! loan
  }
}


class UpdateActor() extends Actor {
  val idSet=new mutable.HashSet[String]()
  def receive = {
    case  loan:LoanText =>
      if(idSet.contains(loan.ListingId)){
        new LoanText(loan.id, text = "").update("id", "text")
        idSet.remove(loan.ListingId)
        DataSave.dealCount += 1
      }else{
        idSet.add(loan.ListingId)
      }
  }
}

case class File(id: String, data: String)

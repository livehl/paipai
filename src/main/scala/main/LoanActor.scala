package main

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import common._
import common.Tool._
import db._
import tools.NetTool

/**
  * Created by admin on 2/23/2017.
  */
class LoanActor(user:ActorRef)  extends Actor with ActorLogging  {
  def receive = {
    case loans:List[Loan] =>
      safe {
        val dbLoans=if(loans.isEmpty) (0::Nil).toSet[Int] else  new Loan().query(s"ListingId in (${loans.map(_.ListingId).mkString(",")})").map(_.ListingId).toSet[Int]
        loans.filter(_.Rate>=20).filter(v=> !dbLoans.contains(v.ListingId)).sortBy(_.Rate * -1).map{loan=>
          val id=loan.insert()
          val html=loanInfo(loan.ListingId,loan.Title)
          if(canBid(loan,html)){
            user ! loan
          }
        }
      }
    case uq: UnSupportQueryExcepiton =>
      log.error(uq, "收到一个来自自身或者其他服务的不支持请求")
    case a: Any =>
      sender ! new UnSupportExcepiton
  }

  def canBid(loan:Loan,html:String):Boolean={
    //审核逾期信息
    val start=html.indexOf("正常还清次数")
    if(start < 0) return false
    def getExpNum(exp:String)={
      val cutStart=html.indexOf(exp)
      html.substring(cutStart+exp.length+1,html.indexOf("</p>",cutStart)).replace("""<span class="num">""","").replace("</span>","").replace("次","").trim()
    }
    val List(count,yu,hei)=List("正常还清次数","逾期(0-15天)还清次数","逾期(15天以上)还清次数").map(v=>getExpNum(v).toInt)
    //没有成功还款过或者有过逾期15天的记录或者逾期次数大于借款数的1/10
    if(count==0 || hei>0 || yu> (count/10)) return false
    if(loan.Funding<100){
      true
    }else false
  }

  def loanInfo(id:Int,title:String)={
    val cookie=PaiPaiUser.getUserCookie
    val data=NetTool.HttpGet("http://invest.ppdai.com/loan/info?id="+id,cookie)._2
    new LoanData(id,title,data,new Date()).insert()
    run {
      Aliyun.saveFile("loan/" + id, data.getBytes("utf-8"))
    }
    data
  }

}

package sdk

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import common.Tool._
import common._
import db._

/**
  * Created by admin on 2/23/2017.
  */
class LoanActor(user:ActorRef)  extends Actor with ActorLogging  {
  def receive = {
    case loans:List[Loan] =>
      safe {
        println(new Date().sdatetime+" acting loans:"+loans.size +",id:"+loans.map(_.ListingId).mkString(","))
        val dbLoans=if(loans.isEmpty) (0::Nil).toSet[Int] else  new Loan().query(s"ListingId in (${loans.map(_.ListingId).mkString(",")})").map(_.ListingId).toSet[Int]
        val newLoans=loans.filter(_.Rate>=20).filter(v=> !dbLoans.contains(v.ListingId)).sortBy(_.Rate * -1)
        val loanInfos=LoansApi.getLoanInfo(newLoans.map((_.ListingId)))
        val loansMap=newLoans.map(v=> v.ListingId -> v).toMap
        val canBidLoans=loanInfos.filter(v=>canBid(loansMap(v.ListingId),v))
        user ! canBidLoans.map(v=>loansMap(v.ListingId))
        DBEntity.transaction{
          newLoans.foreach(_.insert())
          loanInfos.foreach(_.insert())
        }{ex=>
          ex.printStackTrace()
        }
      }
    case uq: UnSupportQueryExcepiton =>
      log.error(uq, "收到一个来自自身或者其他服务的不支持请求")
    case a: Any =>
      sender ! new UnSupportExcepiton
  }
//
  def canBid(loan:Loan,loanInfo: LoanInfo):Boolean={
    //审核逾期信息
    if(loanInfo.NormalCount < 0) return false
    val List(count,yu,hei)=List(loanInfo.NormalCount,loanInfo.OverdueLessCount,loanInfo.OverdueMoreCount)
    //没有成功还款过或者有过逾期15天的记录或者逾期次数大于借款数的1/10
    if(count==0 || hei>0 || yu> (count/10)) return false
    if(loan.Funding<100){
      true
    }else false
  }

}

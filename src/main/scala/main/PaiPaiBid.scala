package main

import java.util.Date

import common.Cache
import common.Tool._
import db._
import org.apache.http.client.CookieStore
import tools.NetTool

/**
  * Created by admin on 2016/9/9.
  */
object PaiPaiBid {
  def main(args: Array[String]) {
  }
  /**
    * 投标策略
    *
    * @param uid
    * @param amount
    * @return
    */
  def bid(uid:Int,amount:BigDecimal)={
    val loans=cacheMethodString("bidLoans",60* 10) {
      PaiPaiLoans.catchPage(100, true).filter { v =>
//        v.Title.contains("次") && !v.Title.contains("第1次") && !v.Title.contains("首次") &&
          v.Rate >= 20
      }.sortBy(_.Rate * -1)
    }
    loans.foldLeft(false){(ret,loan)=> if(ret) ret else bidLoan(uid,amount.toInt,loan.ListingId,loan.Amount.toInt)}
//    val loans=new Loan().query(s"createTime > '${new Date().sdate}' and Title LIKE '%次%'  and Title not LIKE '%第1次%' and  Title not LIKE '%首次%'  and Rate >=20 and Funding < 100   order BY Rate desc,CreditCode,Months,Amount desc,Funding  limit 100 ")

  }
  /**
    * 快速投标策略
    *
    * @param uid
    * @param amount
    * @return
    */
  def quickBid(uid:Int,amount:BigDecimal)={
    val loans=cacheMethodString("bidLoans",60) {
      PaiPaiLoans.catchPage(10, true).filter(_.Rate >=20).sortBy(_.Rate * -1)
    }
    loans.foldLeft(false){(ret,loan)=> if(ret) ret else bidLoan(uid,amount.toInt,loan.ListingId,loan.Amount.toInt)}
  }

  /**
    * 投标
    *
    * @param uid
    * @param amount
    * @param lid
    * @param maxMoney
    * @return
    */
  def bidLoan(uid:Int,amount:Int,lid:Int,maxMoney:Int):Boolean={
    val notBid=new Bid().query("uid=? and lid=?",uid,lid).size == 0
    if(!notBid) return false
    //审核逾期信息
    val fullData=new LoanText().queryOne("ListingId=?",lid).map(_.text)
    if(fullData.isEmpty) return false
    val cutStart=fullData.get.indexOf("<p>正常还清")
    if(cutStart < 0) return false
    val txt=fullData.get.substring(cutStart+3,fullData.get.indexOf("</p>",cutStart))
    val List(count,yu,hei)=txt.split("，").map(v=> v.split("：").last.trim.dropRight(1).trim.toInt).toList
    //没有成功还款过或者有过逾期15天的记录或者逾期次数大于借款数的1/10
    if(count==0 || hei>0 || yu> (count/10)) return false
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return false
    val  funding=PaiPaiLoans.checkLoan(lid)
    if(funding<100){
      val (_,html)=NetTool.HttpPost("http://m.invest.ppdai.com/Listing/BuyHotListingByListingId",cookie.get.asInstanceOf[CookieStore],Map("ListingId"->lid.toString,"amount"->amount.toString,"MaxAmount"->maxMoney.toString))
      new Bid(0,uid,lid,amount,new Date()).insert()
      println(s"bid:${amount}")
      html.contains("成功")
    }else false
  }

}

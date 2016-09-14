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
    * @param uid
    * @param amount
    * @return
    */
  def bid(uid:Int,amount:BigDecimal)={
    val loans=PaiPaiLoans.catchPage(10).filter{v=>
      v.Title.contains("次") && !v.Title.contains("第1次") && !v.Title.contains("首次")  && v.Rate >=20
    }.sortBy(_.Rate * -1 )
    loans.foldLeft(false){(ret,loan)=> if(ret) ret else bidLoan(uid,amount.toInt,loan.ListingId,loan.Amount.toInt)}
//    val loans=new Loan().query(s"createTime > '${new Date().sdate}' and Title LIKE '%次%'  and Title not LIKE '%第1次%' and  Title not LIKE '%首次%'  and Rate >=20 and Funding < 100   order BY Rate desc,CreditCode,Months,Amount desc,Funding  limit 100 ")

  }

  /**
    * 投标
    * @param uid
    * @param amount
    * @param lid
    * @param maxMoney
    * @return
    */
  def bidLoan(uid:Int,amount:Int,lid:Int,maxMoney:Int)={
    val  funding=PaiPaiLoans.checkLoan(lid)
    val cookie=Cache.getCache("user_cookie_"+uid)
    val notBid=new Bid().query("uid=? and lid=?",uid,lid).size == 0
    if(funding<100 && cookie.isDefined && hasBid){
      val (_,html)=NetTool.HttpPost("http://m.invest.ppdai.com/Listing/BuyHotListingByListingId",cookie.get.asInstanceOf[CookieStore],Map("ListingId"->lid.toString,"amount"->amount.toString,"MaxAmount"->maxMoney.toString))
      new Bid(0,uid,lid,amount,new Date()).insert()
      html.contains("成功")
    }else false
  }

}

package main

import java.util.Date

import common.{Aliyun, Cache, OtsCache}
import common.Tool._
import db.{LoanData, _}
import org.apache.http.client.CookieStore
import tools.NetTool

/**
  * Created by admin on 2016/9/9.
  */
object PaiPaiBid {
  def main(args: Array[String]) {
  }

  /**
    * 流投标
    * @param user 用户
    * @param loans  新标的
    * @return 投标数量
    */
  def bidStream(user: UserAccount,loans:List[Loan])={
    val count= ((user.money - user.dayReturnMoney ) /50).toInt
    loans.foldLeft(0){(ret,loan)=> if(ret>=count) ret else if(bidLoan(user.id,50,loan.ListingId,loan.Amount.toInt)) ret +1 else ret }
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
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return false
    val  funding=PaiPaiLoans.checkLoan(lid)
    if(funding<100){
      val (_,html)=NetTool.HttpPost("http://m.invest.ppdai.com/Listing/BuyHotListingByListingId",cookie.get.asInstanceOf[CookieStore],Map("ListingId"->lid.toString,"amount"->amount.toString,"MaxAmount"->maxMoney.toString))
      new Bid(0,uid,lid,amount,new Date()).insert()
      val bidok=html.contains("成功")
      println(s"bid:${amount},${bidok}")
      bidok
    }else false
  }

}

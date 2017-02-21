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
    * 投标策略
    *
    * @param uid
    * @param amount
    * @return
    */
  def bid(uid:Int,amount:BigDecimal)={
    val loans=cacheMethodString("bidLoans",29) {
      PaiPaiLoans.catchPage(10, true).filter(_.Rate >= 20).sortBy(_.Rate * -1)
    }
    loans.foldLeft(false){(ret,loan)=> if(ret) ret else bidLoan(uid,amount.toInt,loan.ListingId,loan.Amount.toInt)}
  }
  /**
    * 快速投标策略
    *
    * @param uid
    * @param amount
    * @return
    */
  def quickBid(uid:Int,amount:Int)={
    val loans=cacheMethodString("bidLoans",29) {
      PaiPaiLoans.catchPage(10, true).filter(_.Rate >=20).sortBy(_.Rate * -1)
    }
    loans.foldLeft(false){(ret,loan)=> if(ret) ret else bidLoan(uid,amount,loan.ListingId,loan.Amount.toInt)}
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
    val cacheData=new LoanData().queryById(lid)
    println(if(cacheData.isDefined) "use nosql" else "use file")
    val fullData=if(cacheData.isDefined) cacheData.map(_.text) else  Aliyun.getFile("loan/"+lid).map(v=> new String(v,"utf-8"))
    if(fullData.isEmpty) return false
    val html=fullData.get
    val start=html.indexOf("正常还清次数")
    if(start < 0) return false
    def getExpNum(exp:String)={
      val cutStart=html.indexOf(exp)
      html.substring(cutStart+exp.length+1,html.indexOf("</p>",cutStart)).replace("""<span class="num">""","").replace("</span>","").replace("次","").trim()
    }
    val List(count,yu,hei)=List("正常还清次数","逾期(0-15天)还清次数","逾期(15天以上)还清次数").map(v=>getExpNum(v).toInt)
    //没有成功还款过或者有过逾期15天的记录或者逾期次数大于借款数的1/10
    if(count==0 || hei>0 || yu> (count/10)) return false
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

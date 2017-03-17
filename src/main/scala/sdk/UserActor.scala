package sdk

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import common.Tool._
import common._
import db._
import org.apache.http.client.CookieStore
import org.jsoup.Jsoup
import tools.{ NetTool}

import scala.collection.mutable

/**
  * Created by admin on 2/23/2017.
  */
class UserActor extends Actor with ActorLogging  {
  val users= new mutable.HashMap()++= (new UserAccount().queryAll().map(v=> v.id->v).toMap)
  def bid(user: UserAccount,loan: Loan)={
    val hasBid=if(user.couponCount>0) bidLoanCoupon(user.uid,50,loan) else bidLoan(user.uid,50,loan)
    println(new Date().sdatetime+" "+loan.ListingId+" "+user.userName.decrypt()+":bid:50,"+hasBid)
    if(hasBid ==0  && (user.money - 50 - user.dayReturnMoney) <= 100){
      self ! user
    }else if(hasBid==0){ //动态修正金额
      users(user.id)=new UserAccount(id=user.id,money=user.money - 50,dayReturnMoney=user.dayReturnMoney,userName = user.userName)
    }
  }
  def receive = {
    case loans:List[Loan] =>
      safe{
        println(new Date().sdatetime+" recv loans:"+loans.size +",id:"+loans.map(_.ListingId).mkString(","))
        loans.foreach{loan=>
          users.map(_._2).filter(v=> v.money - v.dayReturnMoney > 50).map{user=>
            bid(user,loan)
            Thread.sleep(200)
          }
        }
        cacheMethodString("user_account_cache",60){
          users.clear()
          users++= (new UserAccount().queryAll().map(v=> v.id->v).toMap)
        }
      }
    case user:UserAccount=> //更新账户信息
      safe {
        val ck = cacheMethodString("user_cookie_" + user.uid, 3600 * 24) {
          UserApi.login(user.userName.decrypt(), user.passWord.decrypt())
        }
        UserApi.updateUserAccount(user.uid, ck)
        users(user.id)=new UserAccount().queryById(user.id).get
      }
    case uq: UnSupportQueryExcepiton =>
      log.error(uq, "收到一个来自自身或者其他服务的不支持请求")
    case a: Any =>
      sender ! new UnSupportExcepiton
  }

  def bidLoan(uid:Int,amount:Int,loan:Loan):Int={
    val notBid=new Bid().query("uid=? and lid=?",uid,loan.ListingId).size == 0
    if(!notBid) return 1
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return 2
    val funding = 0// PaiPaiLoans.checkLoan(loan.ListingId)
    if (funding < 100) {
      val (_, html) = NetTool.HttpPost("http://m.invest.ppdai.com/Listing/BuyHotListingByListingId", cookie.get.asInstanceOf[CookieStore], Map("ListingId" -> loan.ListingId.toString, "amount" -> amount.toString, "MaxAmount" -> loan.Amount.toString))
      new Bid(0, uid, loan.ListingId, amount, new Date()).insert()
      val bidok = html.contains("成功")
      if (!bidok) {
        if (html.contains("借款列表不存在或已过期")) {
          println("借款列表不存在或已过期:" + loan.ListingId)
        } else {
          println(html)
        }
        4
      } else 0
    }else 3
  }

  def bidLoanCoupon(uid:Int,amount:Int,loan:Loan):Int={
    val notBid=new Bid().query("uid=? and lid=?",uid,loan.ListingId).size == 0
    if(!notBid) return 1
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return 2
    val funding =0// PaiPaiLoans.checkLoan(loan.ListingId)
    if (funding < 100) {
      val (cid,ccode,cmoney)=Cache.getCache("user_coupon"+uid).getOrElse{
        val (ck,chtml)= LoansApi.loanLock.synchronized {
          NetTool.HttpGet(s"http://invest.ppdai.com/bid/info?source=2&listingId=${loan.ListingId}&title=&date=${loan.Months}&UrlReferrer=1&money=${amount}",cookie.get.asInstanceOf[CookieStore])
        }
        Thread.sleep(300)
        val coupon=Jsoup.parse(chtml).select("#couponSelect")
        if(coupon.html()contains("activityid")){
          val op=coupon.select("option").get(0)
          (op.attr("activityid"),op.attr("value"),op.attr("couponamount"))
        }else ("","","")
      }.asInstanceOf[Tuple3[String,String,String]]
      val (_, html) =LoansApi.loanLock.synchronized {
        NetTool.HttpPost("http://invest.ppdai.com/Bid/Bid", cookie.get.asInstanceOf[CookieStore], Map("Reason"->"",   "ListingId" -> loan.ListingId.toString, "Amount" -> amount.toString,
          "UrlReferrer" ->"1","CouponCode"->ccode,"CouponAmount"->cmoney,"ActivityId"->cid,"SubListType"->"0"))
      }
      new Bid(0, uid, loan.ListingId, amount, new Date()).insert()
      val bidok = html.contains("成功")
      if (!bidok) {
        if (html.contains("借款列表不存在或已过期")) {
          println("借款列表不存在或已过期:" + loan.ListingId)
        } else {
          println(html)
        }
        4
      }else{
        Cache.delCache("user_coupon"+uid)
        0
      }
    }else 3
  }

}

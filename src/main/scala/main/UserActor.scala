package main

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import common.Tool._
import common._
import db._
import org.apache.http.client.CookieStore
import org.jsoup.Jsoup
import tools.{Image, NetTool}

import scala.collection.mutable
import scala.util.Random

/**
  * Created by admin on 2/23/2017.
  */
class UserActor extends Actor with ActorLogging  {
  val users= new mutable.HashMap()++= (new UserAccount().queryAll().map(v=> v.id->v).toMap)
  def receive = {
    case loan:Loan =>
      safe {
        users.map(_._2).filter(v=> v.money - v.dayReturnMoney > 100).map{user=>
          val hasBid=bidLoan(user.uid,50,loan)
          println(user.userName.decrypt()+":bid:50,"+hasBid)
          if(hasBid  && (user.money - 50 - user.dayReturnMoney) <= 100){
              self ! user
          }else if(hasBid){ //动态修正金额
            users(user.id)=new UserAccount(id=user.id,money=user.money - 50,dayReturnMoney=user.dayReturnMoney,userName = user.userName)
          }
          Thread.sleep(1000)
        }
      }
    case user:UserAccount=> //更新账户信息
      safe {
        val ck = cacheMethodString("user_cookie_" + user.uid, 3600 * 24) {
          login(user.userName.decrypt(), user.passWord.decrypt())
        }
        updateUserAccount(user.uid, ck)
        users(user.id)=new UserAccount().queryById(user.id).get
      }
    case uq: UnSupportQueryExcepiton =>
      log.error(uq, "收到一个来自自身或者其他服务的不支持请求")
    case a: Any =>
      sender ! new UnSupportExcepiton
  }

  def bidLoan(uid:Int,amount:Int,loan:Loan):Boolean={
    val notBid=new Bid().query("uid=? and lid=?",uid,loan.ListingId).size == 0
    if(!notBid) return false
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return false
    val (_,html)=NetTool.HttpPost("http://m.invest.ppdai.com/Listing/BuyHotListingByListingId",cookie.get.asInstanceOf[CookieStore],Map("ListingId"->loan.ListingId.toString,"amount"->amount.toString,"MaxAmount"->loan.Amount.toString))
    new Bid(0,uid,loan.ListingId,amount,new Date()).insert()
    val bidok=html.contains("成功")
    if(!bidok){
      println(html)
    }
    bidok
  }

  def login(user:String,pwd:String)={
    val (c1,_)=NetTool.HttpGet("http://www.ppdai.com/")
    val (c2,_)=NetTool.HttpGet("https://ac.ppdai.com/User/Login?Redirect=http://www.ppdai.com/",c1)
    val (c3,d)=NetTool.HttpPost("https://ac.ppdai.com/User/Login",c2,Map("IsAsync"->"true","UserName"->user,"Password"->pwd,"Redirect"->"http://m.ppdai.com/Users/Route"))
    println(d)
    val data=d.jsonToMap
    if(data("Content").asInstanceOf[Map[String,Any]]("ShowImgValidateCode").asInstanceOf[Boolean]){
      val (c4,imageData)=NetTool.HttpGetBin("https://ac.ppdai.com/ValidateCode/Image?tmp="+Random.nextDouble(),c3)
      val code=Image.getImageCode(imageData)
      val (c5,d2)=NetTool.HttpPost("https://ac.ppdai.com/User/Login",c4,Map("IsAsync"->"true","ValidateCode"->code,"RememberMe"->"true", "UserName"->user,"Password"->pwd,"Redirect"->"http://m.ppdai.com/Users/Route"))
      println(code+":"+d2)
      c5
    }else  c3
  }
  def updateUserAccount(uid:Int,cookie:CookieStore)={
    val htmlData=NetTool.HttpGet("http://m.invest.ppdai.com/user/UserProfitCenter",cookie)._2
    val html=Jsoup.parse(htmlData)
    val money=html.select(".account-balance .numble")
    val (allMoney,account)=(money.last().text().toBigDecimal,money.first().text().toBigDecimal)
    new UserAccount(uid=uid,money=account,allMoney=allMoney).update("uid","money","allMoney")
    (allMoney,account)
  }

}

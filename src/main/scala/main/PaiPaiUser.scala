package main

import java.util.Date

import com.typesafe.config.ConfigFactory
import common.Tool._
import db._
import org.apache.http.client.CookieStore
import org.jsoup.Jsoup
import tools._

import scala.collection.JavaConverters._

/**
  * Created by isaac on 16/3/9.
  */
object PaiPaiUser {
  lazy val conf = ConfigFactory.load()

  def main(args: Array[String]) {
    checkUsers
//
//    val cookie=login("livehl@126.com","hl890218")
//    val (allMoney,account)=updateUserAccount(cookie)
//    println(account)
  }

  /**
    * 开始用户抓取策略
    */
  def collectUser(){
    val collect=conf.getObjectList("paipai.usertask").asScala.map(v=> (v.get("time").render().toInt ,v.get("action").unwrapped())).toList
    println(collect)
    while (true){
      val i=System.currentTimeMillis()/1000
      collect.foreach{kv=>
        if(i% kv._1 ==0){
          kv._2 match{
            case "check" =>run(checkUsers())
          }
        }
      }
      Thread.sleep(1000)
    }
  }

  /**
    * 用户登陆
    *
    * @param user
    * @param pwd
    * @return
    */
  def login(user:String,pwd:String)={
      val (c1,_)=NetTool.HttpGet("http://m.ppdai.com/lend/Home/Index")
    val (c2,_)=NetTool.HttpGet("https://ac.ppdai.com/User/Login?Redirect=http://m.ppdai.com/lend/User/UserProfitCenter",c1)
    val (c3,d)=NetTool.HttpPost("https://ac.ppdai.com/User/Login",c2,Map("IsAsync"->"true","UserName"->user,"Password"->pwd,"Redirect"->"http://m.ppdai.com/Users/Route"))
    println(d)
    c3
  }

  /**
    * 检查账户资金
    */
  def checkUsers(){
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println(v.userName.decrypt())
      val cookie=cacheMethodString("user_cookie_"+v.uid,3600*5){login(v.userName.decrypt(),v.passWord.decrypt())}
      val (allMoney,account)=updateUserAccount(v.uid,cookie)
      println(v.userName.decrypt()+":"+account)
      if(account>= BigDecimal(50)){
        // 触发投标业务
        val amount=if(account>=BigDecimal(100)) BigDecimal(50) else account
        PaiPaiBid.bid(v.uid,amount)
        updateUserAccount(v.uid,cookie)
      }
      Thread.sleep(1000)
      }
  }

  /**
    * 更新账户信息
    * @param uid
    * @param cookie
    * @return
    */
  def updateUserAccount(uid:Int,cookie:CookieStore)={
      val htmlData=NetTool.HttpGet("http://m.invest.ppdai.com/user/UserProfitCenter",cookie)._2
      val html=Jsoup.parse(htmlData)
      val money=html.select(".account-balance .numble")
      val (allMoney,account)=(money.last().text().toBigDecimal,money.first().text().toBigDecimal)
      new UserAccount(uid=uid,money=account,allMoney=allMoney).update("uid","money","allMoney")
      (allMoney,account)
  }
  //随机获取一个用户cookie
  def getUserCookie={
    val user=new UserAccount().queryAll().head
     cacheMethodString("user_cookie_"+user.uid,3600*12){login(user.userName.decrypt(),user.passWord.decrypt())}
  }


}

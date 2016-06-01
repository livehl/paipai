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
//
    val cookie=login("livehl@126.com","hl890218")
    println(userInfo(cookie))
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

  def userInfo(cookie:CookieStore)={
    val htmlData=NetTool.HttpGet("http://invest.ppdai.com/account/lend",cookie)._2
      val html=Jsoup.parse(htmlData)
      val lendMoney=html.select(".my-ac-c1-two").text().drop(1)
      val amount=html.select(".udrtsmouny em").text().drop(1)
    (lendMoney,amount)
  }


}

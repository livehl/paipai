package main

import java.util.Date

import com.typesafe.config.ConfigFactory
import common.TimeTool
import common.Tool._
import db._
import org.apache.http.client.CookieStore
import org.jsoup.Jsoup
import tools._

import scala.collection.JavaConverters._
import scala.util.Random

/**
  * Created by isaac on 16/3/9.
  */
object PaiPaiUser {
  lazy val conf = ConfigFactory.load()

  def main(args: Array[String]) {
//    checkUsers
//    checkBorrowUsers
    updateUsers
//    val cookie=cacheOTS("testLogin"){login("livehl@126.com","hl890218")}
//    val (allMoney,_,account)=updateUserBorrowAccount(1,cookie)
//    println(account)
    checkUsers
    System.exit(0)
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
            case "borrowCheck"=>run(checkBorrowUsers)
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

  /**
    * 检查账户资金
    */
  def checkUsers(){
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println(v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      val (allMoney,account)=updateUserAccount(v.uid,ck)
      println("update account:"+v.userName.decrypt()+":"+account)
      Thread.sleep(1000)
      }
  }
  //更新账户余额
  def updateUsers(){
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println(v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      updateUserAccount(v.uid,ck)
      Thread.sleep(1000)
    }
  }
  /**
    * 检查贷款账户资金
    */
  def checkBorrowUsers(){
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println("check borrow:"+v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      val (_,canBorrowMoney,_)=updateUserBorrowAccount(v.uid,ck)
      if(canBorrowMoney>10000){
         PaiPaiBorrow.borrow(v.uid,10000)
      }
    }
  }
  //用户流
  def userStream(list:List[Loan]) ={
    val users=new UserAccount().queryAll().filter(v=> v.money - v.dayReturnMoney > 100)
    users.map{user=>
      val count=PaiPaiBid.bidStream(user,list)
      println(user.userName.decrypt()+":bid:"+count+",money:"+count*50)
      if(count>0  && (user.money -count * 50 - user.dayReturnMoney) <= 100){
        val ck=cacheMethodString("user_cookie_"+user.uid,3600*24){login(user.userName.decrypt(),user.passWord.decrypt())}
        updateUserAccount(user.uid,ck)
        Thread.sleep(500)
      }else if(count>0){ //动态修正金额
        new UserAccount(id=user.id,money=user.money - count * 50).update("id","money")
      }
    }

  }

  /**
    * 更新账户信息
    *
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
  /**
    * 更新借款账户信息
    *
    * @param uid
    * @param cookie
    * @return
    */
  def updateUserBorrowAccount(uid:Int,cookie:CookieStore)={
    val html=Jsoup.parse(NetTool.HttpGet("http://invest.ppdai.com/account/lend",cookie)._2)
    val money=html.select(".my-ac-ctListall em").get(2)
    val allBorrowMoney=money.text().drop(1).replace(",","").toBigDecimal
    val canBorrowhtml=Jsoup.parse(NetTool.HttpGet("http://loan.ppdai.com/borrow/createlist/6",cookie)._2)
    val canBorrowMoneyHtml=canBorrowhtml.select(".my-ac-balanceNum")
    val canBorrowMoney=canBorrowMoneyHtml.text().replace(",","").toBigDecimal
    val dayReturnHtml=Jsoup.parse(NetTool.HttpGet("http://loan.ppdai.com/account/repaymentlist",cookie)._2)
    //记录贷款业务
    val borrows=new Borrow().query("uid=?",uid).map(_.lid).toSet
    dayReturnHtml.select(".repaypublist").asScala.map{tab=>
        val allMoney=tab.select(".moneyCount").text().drop(1).replaceAll(",","").toBigDecimal
      val dayMoney=tab.select(".moneyCurrent").text().drop(1).replaceAll(",","").toBigDecimal
      val info=tab.select(".letterspacing").text()
      val date=tab.select(".info").asScala.head.text()
      val lid=tab.select(".repaymentBtn").asScala.head.attr("href").split("/").last
      if(!borrows.contains(lid.toInt)) {
        new Borrow(0, uid, lid.toInt, dayMoney, allMoney, TimeTool.parseStringToDate(date), info, new Date()).insert()
      }else{
        new Borrow(0, uid, lid.toInt, dayMoney, allMoney, TimeTool.parseStringToDate(date), info, new Date()).update("lid","money","info","returnDate")
      }
    }
    val returnList=dayReturnHtml.select(".repaypublist tr").asScala.filter(v=> v.select(".info").size()>0 &&v.select(".info").first().text().toDate.before("1d".dateExp))
    val dayReturnMoney=returnList.map(_.select(".moneyCurrent").text().drop(1).toBigDecimal).sum
    //还钱
    var hasMoney=new UserAccount().queryById(uid).dbCheck.money
    dayReturnHtml.select(".repaypublist tr").asScala.filter(v=> v.select(".info").size()>0 &&v.select(".info").first().text().toDate.before(new Date())).foreach{tr=>
        val rmoney= tr.select(".moneyCurrent").text().drop(1).toBigDecimal
        if(rmoney <= hasMoney){
          val ret=PaiPaiBorrow.repayment(uid,tr.select(".repaymentBtn").attr("href").split("/").last)
           if(ret){
             hasMoney= hasMoney - rmoney
           }
        }
    }
    new UserAccount(uid=uid,allBorrowMoney=allBorrowMoney,canBorrowMoney=canBorrowMoney,dayReturnMoney = dayReturnMoney).update("uid","allBorrowMoney","canBorrowMoney","dayReturnMoney")
    (allBorrowMoney,canBorrowMoney,dayReturnMoney)
  }
  //随机获取一个用户cookie
  def getUserCookie={
    val user=new UserAccount().queryAll().head
     cacheMethodString("user_cookie_"+user.uid,3600*24){login(user.userName.decrypt(),user.passWord.decrypt())}
  }


}

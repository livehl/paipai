package sdk

import java.util.Date

import common.{Cache, TimeTool}
import common.Tool.cacheMethodString
import db.{BackList, Borrow, RetList, UserAccount}
import org.apache.http.client.CookieStore
import org.jsoup.Jsoup
import tools.{Image, NetTool}
import common.Tool._
import org.apache.http.entity.{ContentType, StringEntity}

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Random

/**
  * Created by admin on 3/17/2017.
  */
class UserApi
object UserApi {

  /**
    * 检查账户资金
    */
  def checkUsers()={
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println(v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      val (allMoney,account,coupon)=updateUserAccount(v.uid,ck)
      println("update account:"+v.userName.decrypt()+":"+account+",coupon:"+coupon)
      Thread.sleep(1000)
    }
    "ok,"+users.size
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
    val allMoney=money.last().text().toBigDecimal
    val account=Jsoup.parse(NetTool.HttpGet("http://invest.ppdai.com/account/userstatistics",cookie)._2).select(".my-ac-ps-yue").text().drop(1).replace(",","").toBigDecimal
    val count=Jsoup.parse(NetTool.HttpGet("http://www.ppdai.com/account/coupon",cookie)._2).select(".tableStyleOne tr").asScala.filter(v=> !v.html().contains("借款") && !v.html().contains("没有优惠券")).size -1
    new UserAccount(uid=uid,money=account,allMoney=allMoney,couponCount = count).update("uid","money","allMoney","couponCount")
    (allMoney,account,count)
  }

  def updateUserAccountOld(uid:Int,cookie:CookieStore)={
    val htmlData=NetTool.HttpGet("http://m.invest.ppdai.com/user/UserProfitCenter",cookie)._2
    val html=Jsoup.parse(htmlData)
    val money=html.select(".account-balance .numble")
    val (allMoney,account)=(money.last().text().toBigDecimal,money.first().text().toBigDecimal)
    val count=Jsoup.parse(NetTool.HttpGet("http://www.ppdai.com/account/coupon",cookie)._2).select(".tableStyleOne tr").asScala.filter(v=> !v.html().contains("借款")).size -1
    new UserAccount(uid=uid,money=account,allMoney=allMoney,couponCount = count).update("uid","money","allMoney","couponCount")
    (allMoney,account,count)
  }
  /**
    * 检查贷款账户资金
    */
  def checkBorrowUsers()={
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println("check borrow:"+v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      val (_,canBorrowMoney,_)=updateUserBorrowAccount(v.uid,ck)
      if(canBorrowMoney != null && canBorrowMoney > BigDecimal(10000)){
        borrow(v.uid,BigDecimal(10000))
      }
    }
    "ok,"+users.size
  }

  /**
    * 更新借款账户信息
    *
    * @param uid
    * @param cookie
    * @return
    */
  def updateUserBorrowAccount(uid:Int,cookie:CookieStore)={
    val html=LoansApi.loanLock.synchronized {
      Jsoup.parse(NetTool.HttpGet("http://invest.ppdai.com/account/lend",cookie)._2)
    }
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
        val ret=repayment(uid,tr.select(".repaymentBtn").attr("href").split("/").last)
        if(ret){
          hasMoney= hasMoney - rmoney
        }
      }
    }
    new UserAccount(uid=uid,allBorrowMoney=allBorrowMoney,canBorrowMoney=canBorrowMoney,dayReturnMoney = dayReturnMoney).update("uid","allBorrowMoney","canBorrowMoney","dayReturnMoney")
    (allBorrowMoney,canBorrowMoney,dayReturnMoney)
  }

  /**
    * 借款
    *
    * @param uid
    * @param amount
    * @return
    */
  def borrow(uid:Int,amount:BigDecimal):Boolean={
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return false
    val body=new StringEntity(s"""{"Type":6,"ListAmount":"${amount}","Months":"12","Rate":"8.01","Description":"一定要标准的十个字么","BorrowCredit":13,"UseService":false,"UseFenQi":false}""",ContentType.APPLICATION_JSON)
    val (_,ret)=NetTool.HttpPost("http://loan.ppdai.com/Json/SyncReply/CreateList",cookie.get.asInstanceOf[CookieStore],appendHead = Map("Content-Type"->"application/json"),entity = body)
    //记录贷款业务
    //new Borrow(0,uid,0,amount.toInt,null,new Date()).insert()
    ret.contains("发布成功")
  }
  // 还钱
  def repayment(uid:Int,lid:String):Boolean={
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return false
    val body=new StringEntity(s"""{"PaybackType":"1","ListingId":"${lid}"}""",ContentType.APPLICATION_JSON)
    val (_,ret)=NetTool.HttpPost("http://loan.ppdai.com/Json/SyncReply/ListPaymentRequest",cookie.get.asInstanceOf[CookieStore],appendHead = Map("Content-Type"->"application/json"),entity = body)
    ret.contains("success")
  }
  /**
    * 领取蚊子肉
    */
  def checkUsersCoupon()={
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println("check Coupon:"+v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      println(getUserCoupon(ck))
    }
    "ok,"+users.size
  }

  def getUserCoupon(cookie:CookieStore)={
    val ck=NetTool.HttpGet("https://m.invest.ppdai.com/Activity/Calendar?mType=Group",cookie)._1
    val ret=NetTool.HttpPost("https://m.invest.ppdai.com/Activity/InsertCalendar",ck)._2
    ret
  }
  /**
    * 领取蚊子肉
    */
  def checkUsersSign()={
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println("check Sign:"+v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      println(getUserSign(ck))
    }
    "ok,"+users.size
  }

  def getUserSign(cookie:CookieStore)={
    val ck=NetTool.HttpGet("http://weixin.ppdai.com/SignIn",cookie)._1
    val ret=NetTool.HttpPost("http://weixin.ppdai.com/SignIn/SignIn",ck)._2
    ret
  }
  //更新账户余额
  def updateUsers()={
    val users=new UserAccount().queryAll()
    users.foreach { v =>
      println(v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      updateUserAccount(v.uid,ck)
      Thread.sleep(1000)
    }
    "ok,"+users.size
  }
  //获取所有用户的黑名单
  def getAllBackList={
    val users=new UserAccount().queryAll()
    val datas=users.map { v =>
      Thread.sleep(1000)
      println(v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      getBackList(ck)
    }.flatten.toSet
    val backList=new BackList().queryAll().map(_.ListingId).toSet
    datas.filterNot(v=> backList.contains(v)).foreach(v=> new BackList(0,v,new Date()).insert())
    backList.filterNot(v=> datas.contains(v)).foreach(v=> new BackList(0,v,new Date()).delete("ListingId") )
    "ok,"+datas.size
  }
  //获取指定用户的黑名单
  def getBackList(ck:CookieStore)={
    def getAllBackList(num:Int):List[String]= {
      val page=Jsoup.parse(NetTool.HttpGet(s"http://invest.ppdai.com/account/blacklist?PageIndex=${num}&LateDayTo=10&LateDayFrom",ck)._2)
      val table = page.select("table").asScala
      val lines = table.map { tb =>
        tb.select("tr").asScala.map { tr =>
          tr.select("[listingid]").attr("listingid")
        }
      }
      val maxPage = page.select(".pagerstatus").html().drop(1).dropRight(1).trim.toInt
      (if(maxPage> num) getAllBackList(num +1) else Nil ) ::: lines.flatten.toList
    }
    val data=getAllBackList(1)
    println(data)
    data.filterNot(_.isEmpty)
  }

  def getAllRetList={
    val users=new UserAccount().queryAll()
    val datas=users.map { v =>
      Thread.sleep(1000)
      println(v.userName.decrypt())
      val ck=cacheMethodString("user_cookie_"+v.uid,3600*24){login(v.userName.decrypt(),v.passWord.decrypt())}
      getRetList(ck)
    }.flatten.toSet
    val retList=new RetList().queryAll().map(_.ListingId).toSet
    datas.filterNot(v=> retList.contains(v)).foreach(v=> new RetList(0,v,new Date()).insert())
//    retList.filterNot(v=> datas.contains(v)).foreach(v=> new RetList(0,v,new Date()).delete("ListingId") )
    "ok,"+datas.size
  }

  //获取指定用户的还清名单
  def getRetList(ck:CookieStore)={
    def getAllBackList(num:Int):List[String]= {
      val page=Jsoup.parse(NetTool.HttpGet(s"http://invest.ppdai.com/account/paybacklend?pageIndex=${num}&Type=1",ck)._2)
      val table = page.select(".my-paid-list").asScala
      val lines = table.map { tb =>
        tb.select("span").asScala.map { tr =>
          tr.select("[listid]").attr("listid")
        }
      }
      val maxPage = page.select(".pagerstatus").html().drop(1).dropRight(1).trim.toInt
      (if(maxPage> num) getAllBackList(num +1) else Nil ) ::: lines.flatten.toList
    }
    val data=getAllBackList(1)
    println(data)
    data.filterNot(_.isEmpty)
  }
}

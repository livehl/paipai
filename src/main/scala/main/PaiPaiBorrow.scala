package main

import java.util.Date

import common.Tool._
import common.{Cache, OtsCache}
import db._
import org.apache.http.client.CookieStore
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.entity.mime.content.StringBody
import tools.NetTool

/**
  * Created by admin on 2016/9/9.
  */
object PaiPaiBorrow {
  def main(args: Array[String]): Unit = {

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
  //TODO 还钱
  def repayment(uid:Int,lid:String):Boolean={
    val cookie=Cache.getCache("user_cookie_"+uid)
    if(cookie.isEmpty) return false
//    val body=new StringEntity(s"""{"Type":6,"ListAmount":"${amount}","Months":"12","Rate":"8.01","Description":"一定要标准的十个字么","BorrowCredit":13,"UseService":false,"UseFenQi":false}""",ContentType.APPLICATION_JSON)
//    val (_,ret)=NetTool.HttpPost("http://loan.ppdai.com/Json/SyncReply/CreateList",cookie.get.asInstanceOf[CookieStore],appendHead = Map("Content-Type"->"application/json"),entity = body)
//    //记录贷款业务
//    //new Borrow(0,uid,0,amount.toInt,null,new Date()).insert()
//    ret.contains("成功")
    false
  }

}

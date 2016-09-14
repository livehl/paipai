package main

import java.util.Date

import com.typesafe.config.ConfigFactory
import db._
import org.apache.http.client.CookieStore
import org.jsoup.Jsoup
import tools._
import common._
import common.Tool._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by isaac on 16/3/9.
  */
object PaiPaiLoans {
  lazy val conf = ConfigFactory.load()

  def main(args: Array[String]) {
//
//    val cookie=login("livehl@126.com","hl890218")
//    println(cookie)
//    checkLoans
//    catchPage(1)

  }

  /**
    * 启动标的抓取策略
    */
  def collectLoan(){
    val collect=conf.getObjectList("paipai.loantask").asScala.map(v=> (v.get("time").render().toInt , v.get("page").render().toInt,v.get("action").unwrapped())).toList
    println(collect)
    while (true){
      val i=System.currentTimeMillis()/1000
      collect.foreach{kv=>
        if(i% kv._1 ==0){
          kv._3 match{
            case "check" =>run(checkLoans())
            case _=>run(catchPage(kv._2))
          }

        }
      }
      Thread.sleep(1000)
    }
  }

  /**
    * 抓取标的
    *
    * @param page
    */
  def catchPage(page:Int):List[Loan]={
    val buffer=new ArrayBuffer[Loan]()
    1 to page foreach{i=>
      val (cookie,str)=NetTool.HttpPost("http://m.invest.ppdai.com/listing/ajaxindex",null,Map("pageIndex"->i.toString))
      val lists=toBean(str,classOf[List[Map[String,AnyRef]]]).map(v=> new Loan().fromJson(v.toJson))
      lists.filter(_.Rate>12).foreach{loan=>
        buffer.append(loan)
        val dbLoan=loan.query("ListingId=?",loan.ListingId)
        if(dbLoan.isEmpty){
          val id=loan.insert()
          loanInfo(id,loan.ListingId,loan.Title)
        }else{
          new Loan(dbLoan.head.id,Funding = loan.Funding,lastUpdate = new Date()).update("id","Funding","lastUpdate")
        }
      }
      if(lists.size<10){
        return buffer.toList
      }
      println(new Date().sdatetime + " catch page:"+i)
      Thread.sleep(5000)
    }
    println(new Date().sdatetime +" catch page end")
    buffer.toList
  }

  /**
    * 检查所有未完成的标的状态
    */
  def checkLoans(){
      val ids=DBEntity.queryMap(s"select ListingId from ${new Loan().tableName} where Funding < 100 and Funding > 80  and Rate >= 20 and createTime >'"+("-1d".dateExp.sdate)+"'  order by Funding desc ").map(_("ListingId").asInstanceOf[Int])
      println(ids.size)
    ids.grouped(100) foreach { page =>
        println(" check new page:"+page)
        page.mutile(2).foreach { v =>
            checkLoan(v)
          Thread.sleep(500)
        }
      }
  }

  /**
    * 检查标的是否完成
    *
    * @param id
    */
  def checkLoan(id:Int)={
      val data=NetTool.HttpGet("http://m.ppdai.com/lend/"+id)._2
      val jsoup=Jsoup.parse(data)
      val funding=jsoup.select(".earningsLine").text().dropRight(1)
    val info=jsoup.select(".userInfo p").html() + jsoup.select(".verifyInfo p").html()
    new Loan(ListingId = id,Funding = funding.toInt, ext=info,lastUpdate = new Date()).update("ListingId","Funding","ext","lastUpdate")
    funding.toInt
  }


  /**
    * 标的html详情
    * @param id
    */
  def loanInfo(lid:Int,id:Int,title:String){
    val data=NetTool.HttpGet("http://invest.ppdai.com/loan/info?id="+id)._2
    new LoanText(lid,title,id,data,new Date()).insertWithId()
  }

}

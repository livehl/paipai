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
    loanStream

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
          }
        }
      }
      Thread.sleep(1000)
    }
  }

  def loanStream(){
    val cookie=PaiPaiUser.getUserCookie
    var i=1
    while (true){
      val (_, str) = NetTool.HttpPost("http://m.invest.ppdai.com/listing/ajaxindex", cookie, Map("pageIndex" -> i.toString))
      if (!isEmpty(str)) {
        val lists = toBean(str, classOf[List[Map[String, AnyRef]]]).map(v => new Loan().fromJson(v.toJson))
        if(System.currentTimeMillis()/1000 % 30==1){
          println(new Date().sdatetime+"page:"+i+",size:"+lists.size)
        }
        //流
        run(loanSaveStream(lists.filter(v => v.Rate >= 18)))
        if (lists.size < 10) {
          i=1
        }else{
          i+=1
        }
      } else {
        i=1
      }
      Thread.sleep(1000)
    }
  }

  def loanSaveStream(list:List[Loan]){
    val dbLoans=if(list.isEmpty) (0::Nil).toSet[Int] else  new Loan().query(s"ListingId in (${list.map(_.ListingId).mkString(",")})").map(_.ListingId).toSet[Int]
      val loans=list.filter(_.Rate>=20).filter(v=> !dbLoans.contains(v.ListingId)).map{loan=>
        val id=loan.insert()
        loanInfo(loan.ListingId,loan.Title)
        Thread.sleep(300)
        loan
      }.sortBy(_.Rate * -1)
    //流
    if(loans.size>0) {
      println("stream loan:" + loans.size)
      PaiPaiUser.userStream(loans)
    }
  }

  /**
    * 检查所有未完成的标的状态
    */
  def checkLoans(){
      val ids=DBEntity.queryMap(s"select ListingId from ${new Loan().tableName} where Funding < 100 and Funding > 80  and Rate >= 20 and createTime >'"+("-1d".dateExp.sdate)+"'  order by Funding desc ").map(_("ListingId").asInstanceOf[Int])
      println(ids.size)
    ids.grouped(100) foreach { page =>
        println(" check new page:"+page)
        page.foreach { v =>
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
    new Loan(ListingId = id,Funding = funding.toInt,lastUpdate = new Date()).update("ListingId","Funding","lastUpdate")
    funding.toInt
  }


  /**
    * 标的html详情
    *
    * @param id
    */
  def loanInfo(id:Int,title:String){
    val cookie=PaiPaiUser.getUserCookie
    val data=NetTool.HttpGet("http://invest.ppdai.com/loan/info?id="+id,cookie)._2
    new LoanData(id,title,data,new Date()).insert()
    Aliyun.saveFile("loan/"+id,data.getBytes("utf-8"))
    data
  }

}

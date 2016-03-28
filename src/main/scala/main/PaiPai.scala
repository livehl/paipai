package main

import java.util.Date

import com.typesafe.config.ConfigFactory
import db._
import org.jsoup.Jsoup
import tools._
import common._
import common.Tool._
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created by isaac on 16/3/9.
  */
object PaiPai {
  lazy val conf = ConfigFactory.load()

  def main(args: Array[String]) {

    val cookie=login("livehl@126.com","890218")
    println(cookie)

//    collectLoan


  }

  def collectLoan(){
    val collect=conf.getObjectList("paipai.task").asScala.map(v=> (v.get("time").render().toInt , v.get("page").render().toInt,v.get("action").render())).toList
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
  def catchPage(page:Int){
    1 to page foreach{i=>
      val (cookie,str)=NetTool.HttpPost("http://m.ppdai.com/lend/listing/ajaxindex",null,Map("pageIndex"->i.toString))
      val lists=toBean(str,classOf[List[Map[String,AnyRef]]]).map(v=> new Loan().fromJson(v.toJson))
      lists.mutile(5).foreach{loan=>
        val dbLoan=loan.query("ListingId=?",loan.ListingId)
        if(dbLoan.isEmpty){
          loan.insert()
        }else{
          new Loan(dbLoan.head.id,Funding = loan.Funding,lastUpdate = new Date()).update("id","Funding","lastUpdate")
        }
      }
      if(lists.size<10){
        return
      }
      println(new Date().sdatetime + "page:"+i)
    }
    println(new Date().sdatetime +"page end")
  }

  /**
    * 检查所有未完成的标的状态
    */
  def checkLoans(){
      val size=DBEntity.count(s"select count(*) from ${new Loan().tableName} where Funding < 100")
      println(size)
      val hashSet=new mutable.HashSet[String]()
      1 to (size/100+1) foreach { page =>
        val num=if(hashSet.size==0) 1 else (hashSet.size/100+1)
        val ads = new Loan().queryPage("Funding < 100",num, 100, "lastUpdate asc")._2
        ads.mutile(3).foreach { v =>
          if(!hashSet.contains(v.ListingId)){
            checkLoan(v.ListingId)
            hashSet.add(v.ListingId)
          }
        }
        println(num)
      }
  }

  /**
    * 检查标的是否完成
    *
    * @param id
    */
  def checkLoan(id:Int){
      val data=NetTool.HttpGet("http://m.ppdai.com/lend/"+id)._2
      val jsoup=Jsoup.parse(data)
      val funding=jsoup.select(".earningsLine").text().dropRight(1)
    val info=jsoup.select(".userInfo p").html()
    new Loan(ListingId = id,Funding = funding.toInt,ext=info,lastUpdate = new Date()).update("ListingId","Funding","ext","lastUpdate")
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
    val (c3,d)=NetTool.HttpPost("https://ac.ppdai.com/User/Login",c2,Map("IsAsync"->"true","UserName"->user,"Password"->pwd,"Redirect"->"http://m.ppdai.com/lend/User/UserProfitCenter"))
    println(d)
    c3
  }


}

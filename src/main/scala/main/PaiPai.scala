package main

import java.util.Date

import com.typesafe.config.ConfigFactory
import db._
import tools._
import common._
import common.Tool._
import scala.collection.JavaConverters._

/**
  * Created by isaac on 16/3/9.
  */
object PaiPai {
  lazy val conf = ConfigFactory.load()

  def main(args: Array[String]) {

    val collect=conf.getObjectList("paipai.collect").asScala.map(v=> v.get("time").render().toInt -> v.get("page").render().toInt).toList
    println(collect)
    while (true){
    val i=System.currentTimeMillis()/1000
      collect.foreach{kv=>
        if(i% kv._1 ==0){
          run(cachePage(kv._2))
        }
      }
      Thread.sleep(1000)
    }



  }
  def cachePage(page:Int){
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


}

package main

import common.Tool._
import common.{Aliyun, OtsCache}
import db._

/**
  * Created by isaac on 16/4/15.
  */
object DataSave {

  def main(array: Array[String]): Unit = {
    val allCount=new LoanText().queryCount("")
    println(allCount)
    1 to allCount/100 foreach{page=>
        println(page)
        new LoanText().queryPage("",page,100,"createTime asc")._2.map{text=>
          val textData= text.text
          if(textData.length>100) {
            new LoanData(text.ListingId.toString, text.Title, textData, text.createTime).insert()
            Aliyun.saveFile("loan/" + text.ListingId, textData.getBytes("utf-8"))
            //校验数据
            val amd5 = new String(Aliyun.getFile("loan/" + text.ListingId).get, "utf-8").md5()
            val otsMd5 = new LoanData().queryById(text.ListingId).get.text.md5
            if (otsMd5 == amd5 && textData.md5 == amd5) {
              new LoanText(text.id, text = "").update("id", "text")
            }
          }
        }
    }
  }
}

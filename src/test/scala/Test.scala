import java.io.{File, FileInputStream}

import common.OtsCache
import main.PaiPaiLoans
import common.Tool._
import db._
import org.apache.http.entity.ByteArrayEntity
import tools.{Image, NetTool}

import scala.io.Source

/**
  * Created by isaac on 16/4/15.
  */
object Test {



  def main(array: Array[String]): Unit = {
    OtsCache.setCache("nimei","你妹".getBytes)
    println(new String(OtsCache.getCache("nimei").get))
//    val data=File2Byte(new File("z:\\Image.gif"))
//      println(Image.getImageCode(data))
//    val lid="20661086"
//    val fullData=new LoanText().queryOne("ListingId=?",lid).map(_.text).getOrElse("")
//    val cutStart=fullData.indexOf("<p>正常还清")
//    val txt=fullData.substring(cutStart+3,fullData.indexOf("</p>",cutStart))
//    val List(count,yu,hei)=txt.split("，").map(v=> v.split("：").last.trim.dropRight(1)).toList
//    println(count)
//    println(yu)
//    println(hei)
//    println(List(1,6,3,4,5).sortBy(v=> v * -1))
//    new Setting(-1,"","","").insert()
//    new Bid().createTable()
//    println("-2d".dateExp.sdate)
//    val entity = new ByteArrayEntity("""{"Borrowernumber":"24069614","UserId":0,"listingId":"19129600"}""".getBytes("UTF-8"))
//    val v=NetTool.HttpPost("http://wirelessgateway.ppdai.com/Invest/BorrowerinfoService/Borrowerinfo",entity=entity)
//    println(v._2)
//    PaiPaiLoans.loanInfo(19286556)
  }
}

import java.io.{File, FileInputStream}
import java.util.Date

import common.{OtsCache, TimeTool}
import common.Tool._
import db._
import org.apache.http.entity.ByteArrayEntity
import tools.{Image, NetTool}

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Created by isaac on 16/4/15.
  */
object Test {



  def main(array: Array[String]): Unit = {

//    runTasks

    NetTool.HttpPost("http://ppdai.local/predict")

//    println("1d".dateExp.sdatetime)
//    println(new TaskLog().createTable())
//    val html="""<p class="tab-hd"><span></span>借款记录</p>
//               |                    <div class="flex wid720">
//               |                        <p class="ex col-1">注册时间：2016/12/7 </p>
//               |                        <p class="ex col-2 center">历史记录: <span class="num">0</span>次流标，<span class="num">0</span>次撤标，<span class="num">0</span>次失败</p>
//               |                        <p class="ex col-1 center last">成功借款次数：<span class="num">0</span>次</p>
//               |                    </div>
//               |                    <p class="tab-hd" style="margin-top: 10px;"><span></span>还款相关</p>
//               |                    <div class="flex">
//               |                        <p class="ex col-1" style="max-width:158px">成功还款次数: <span class="num">0</span>次</p>
//               |                        <p class="ex col-1 center">正常还清次数: <span class="num">0</span>次</p>
//               |                        <p class="ex col-1 center">逾期(0-15天)还清次数: <span class="num">0</span>次</p>
//               |                        <p class="ex col-1 center last">逾期(15天以上)还清次数：<span class="num">0</span>次</p>
//               |                    </div>
//               |""".stripMargin
//    val start=html.indexOf("正常还清次数")
//    if(start < 0) return false
//    def getExpNum(exp:String)={
//      val cutStart=html.indexOf(exp)
//      html.substring(cutStart+exp.length+1,html.indexOf("</p>",cutStart)).replace("""<span class="num">""","").replace("</span>","").replace("次","").trim()
//    }
//    val List(count,yu,hei)=List("正常还清次数","逾期(0-15天)还清次数","逾期(15天以上)还清次数").map(v=>getExpNum(v).toInt)
//    val l=new LoanData().queryById("683054")
//    System.exit(0)
//    OtsCache.setCache("nimei","你妹".getBytes)
//    println(new String(OtsCache.getCache("nimei").get))
//    val all=OtsCache.getAll
//    all.foreach{d=>
//      d.getColumns
//      println(d.getPrimaryKey.getPrimaryKeyColumns.head.getValue.asString())
//
//    }
//    println(all.size)
//    println(new Borrow(returnDate = new Date()).createTable())
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


  def runTasks(){
    var two=0
    val set=new mutable.HashMap[Int,Int]()
    var hour=8
    var minute=27
    var second=17
    var t=TimeTool.parseStringToDateTime("2017-4-1 00:00:00").getTime
    val tasks=new Task().query("status=0 and id not in (1,4,5)")
    println(tasks.toJson())
    while (true){
      val date=new Date(t)
      if(date.getHours==0 && date.getMinutes==0 && date.getSeconds ==0){ //清除当日执行队列
        set.clear()
        hour=7+Random.nextInt(13)
        minute=10+Random.nextInt(48)
        second=10+Random.nextInt(45)
      }
      val i=t/1000
      tasks.foreach{t=>
        val List(ttype,value)=t.time.split(",").toList
        val isMatch=ttype match{
          case "t"=>  i% value.toInt ==0
          case "rd"=>
            if(!set.contains(t.id)){
              val h=value.split(":").head.toInt +Random.nextInt(value.split(":").last.toInt)
              set(t.id)=h
            }
            val ret=date.getHours==set(t.id)&& date.getMinutes==minute && date.getSeconds ==second
            if(ret) {
              second = second - Random.nextInt(3)
              minute = minute - Random.nextInt(3)
            }
            ret
          case "rm"=>
            if(!set.contains(t.id)){
              val d=value.split(":").head.toInt +Random.nextInt(value.split(":").last.toInt)
              set(t.id)=d
            }
            val ret=date.getDate==set(t.id)&&date.getHours==hour&& date.getMinutes==minute && date.getSeconds ==second
            if(ret) {
              second = second - Random.nextInt(3)
              minute = minute - Random.nextInt(3)
              hour = hour - Random.nextInt(3)
            }
            ret
          case "d"=> date.sdate == value
          case v:Any=> println("unsuper:"+v)
            false
        }
        if(isMatch){
//          run {
            val cl = Class.forName(t.method.split("\\.").dropRight(1).mkString(".")).newInstance()
            val me = cl.getClass.getMethod(t.method.split("\\.").last)
            try {
              val ret = me.invoke(cl)
              new TaskLog(0, t.id, t.time, 0, ret.toString, new Date()).insert()
            } catch {
              case ex: Throwable =>
                ex.printStackTrace()
                new TaskLog(0, t.id, t.time, 1, ex.getMessage, new Date()).insert()
            }
//          }
          two=two +1
        }
        if(two==2) {
          System.exit(0)
        }
      }
      t = t+ 1000
    }
  }
}

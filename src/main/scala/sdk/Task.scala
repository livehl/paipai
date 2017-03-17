package sdk

import java.util.Date

import common.Tool._
import db._

import scala.collection.mutable
import scala.util.Random

/**
  * Created by admin on 3/17/2017.
  */
object Task {

  def main(args: Array[String]): Unit = {
    runTasks
  }

  /**
    * 开始执行定时任务
    */
  def runTasks(){
    val set=new mutable.HashMap[Int,Int]()
    var hour=8
    var minute=27
    var second=17
    def tasks=cacheMethodString("task_cache",60){new Task().query("status=0")}
    println(tasks.toJson())
    while (true){
      val date=new Date()
      if(date.getHours==0 && date.getMinutes==0 && date.getSeconds ==0){ //清除当日执行队列
        set.clear()
        hour=7+Random.nextInt(13)
        minute=10+Random.nextInt(48)
        second=10+Random.nextInt(45)
      }
      val i=System.currentTimeMillis()/1000
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
            second=second - Random.nextInt(3)
            minute=minute - Random.nextInt(3)
            ret
          case "rm"=>
            if(!set.contains(t.id)){
              val d=value.split(":").head.toInt +Random.nextInt(value.split(":").last.toInt)
              set(t.id)=d
            }
            val ret=date.getDate==set(t.id)&&date.getHours==hour&& date.getMinutes==minute && date.getSeconds ==second
            second=second - Random.nextInt(3)
            minute=minute - Random.nextInt(3)
            hour=hour - Random.nextInt(3)
            ret
          case "d"=> date.sdate == value
          case v:Any=> println("unsuper:"+v)
            false
        }
        if(isMatch){
          run {
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
          }
        }
      }
      Thread.sleep(1000)
    }
  }

}

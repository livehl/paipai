package common

import java.io.Serializable
import java.util.{Date, Collections}

import com.typesafe.config.ConfigFactory
import org.apache.commons.collections.map.LRUMap
import tools.Z4ZTool._

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import concurrent.duration._
import scala.collection.JavaConversions._

/**
  * Created by isaac on 16/2/15.
  */
object Cache {
  lazy val conf = ConfigFactory.load()
  //内存易失性缓存
  private val softCache = Collections.synchronizedMap[String, (Long, AnyRef,Boolean)](new SoftHashMap[String, (Long, AnyRef,Boolean)](conf.getInt("cache.softSize")))
  //一级缓存
  private val oneCache=Collections.synchronizedMap[String,AnyRef](new LRUMap(conf.getInt("cache.oneSize")).asInstanceOf[java.util.Map[String,AnyRef]])
  //使用易失性缓存
  private val useSoft=conf.getInt("cache.softSize")>0
  private val useZ4z=conf.getBoolean("cache.z4zString")
   //分别存储到一级缓存\二级缓存
  private def setCacheValue(key: String, v: AnyRef,time:Int)={
    val saveTime = if( time== Int.MaxValue) time else System.currentTimeMillis() / 1000 +time
    val (cacheValue,isZ4z)= if(useZ4z && v.isInstanceOf[String]) (v.asInstanceOf[String].z4z,true) else (v,false)
    val cacheData=new CacheData(cacheValue,time,isZ4z)
      oneCache.put(key, (saveTime, cacheValue, isZ4z))
      if (useSoft) softCache.put(key, (saveTime, cacheValue, isZ4z))
  }
  def getCache(key:String)={
    val now = System.currentTimeMillis() / 1000
    var cv:Tuple3[Long, AnyRef, Boolean]=null
    var scv:Tuple3[Long, AnyRef, Boolean]=null
    cv = oneCache.get(key).asInstanceOf[Tuple3[Long, AnyRef, Boolean]]
    scv= if(useSoft)softCache.get(key).asInstanceOf[Tuple3[Long,AnyRef,Boolean]] else null
    if(cv!=null)
      if (cv._1 <= now) {
        delCache(key)
        None
      }else
      Some(if(cv._3) cv._2.asInstanceOf[Array[Byte]].unz4zStr else cv._2)
    else {
      if(scv!=null){
        if (scv._1 <= now) {
          delCache(key)
          None
        }else
          Some(if(scv._3) scv._2.asInstanceOf[Array[Byte]].unz4zStr else scv._2)
      } else {
        None
      }
    }
  }

  def setCache(key: String, v: AnyRef,time:Int= -1) = {
    setCacheValue(key,v,time)
  }
  def delCache(key: String) = {
    oneCache.remove(key)
    if (useSoft) softCache.remove(key)
  }

}
//用于缓存的数据结构
class CacheData(val value:AnyRef,val time:Long,val z4z:Boolean)extends  Serializable{
  def getValue={
      if(z4z) value.asInstanceOf[Array[Byte]].unz4zStr
      else value
  }
}






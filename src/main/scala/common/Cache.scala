package common

import java.io.Serializable
import java.util.{Collections, Date}

import com.typesafe.config.ConfigFactory
import org.apache.commons.collections.map.LRUMap
import tools.Z4ZTool._

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import concurrent.duration._
import scala.collection.JavaConversions._
import com.alicloud.openservices.tablestore.model._
import com.alicloud.openservices.tablestore._
import tools.ReflectTool

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

object OtsCache {
  lazy val conf = ConfigFactory.load()
  private lazy val client = new SyncClient(conf.getString("ots.url"), conf.getString("ali.accessId"), conf.getString("ali.accessKey"), conf.getString("ots.instanceName"))
  private val cacheTableName=conf.getString("ots.table")
  private val useZ4z=conf.getBoolean("ots.z4z")
  checkTable


  private def checkTable={
    client.listTable()
    if(!client.listTable().getTableNames.contains(cacheTableName)){
      client.listTable().getTableNames.toArray.foreach(println)
      val tableMeta = new TableMeta(cacheTableName)
      tableMeta.addPrimaryKeyColumn("key", PrimaryKeyType.STRING)
      //默认保存10年
      val ttl=if(conf.hasPath("cache.ots.ttl")) conf.getInt("cache.ots.ttl") else 3600*24*3650
      val request = new CreateTableRequest(tableMeta, new TableOptions(ttl, 1))
      request.setReservedThroughput(new ReservedThroughput(0, 0))
      client.createTable(request)
    }
  }

  def setCache(key:String,obj:AnyRef){
    val rowChange = new RowPutChange(cacheTableName)
    val primaryKey =  PrimaryKeyBuilder.createPrimaryKeyBuilder().addPrimaryKeyColumn("key", PrimaryKeyValue.fromString(key)).build()
    rowChange.setPrimaryKey(primaryKey)
    val data=ReflectTool.getBytesByObject(obj)
    rowChange.addColumn("value", ColumnValue.fromBinary(if(useZ4z)data.z4z else data))
    rowChange.addColumn("z4z", ColumnValue.fromBoolean(useZ4z))
    rowChange.setCondition(new Condition(RowExistenceExpectation.IGNORE))
    val request = new PutRowRequest()
    request.setRowChange(rowChange)
    import Tool._
    Tool.reTry(3) {
      val result = client.putRow(request)
      result.getConsumedCapacity().getCapacityUnit().getWriteCapacityUnit()
    }
  }
  def getCache[T](key: String):Option[T] = {
    val now = System.currentTimeMillis() / 1000
    val criteria = new SingleRowQueryCriteria(cacheTableName)
    val primaryKey =PrimaryKeyBuilder.createPrimaryKeyBuilder().addPrimaryKeyColumn("key", PrimaryKeyValue.fromString(key)).build()
    criteria.setPrimaryKey(primaryKey)
    criteria.setMaxVersions(1)
    val request = new GetRowRequest()
    request.setRowQueryCriteria(criteria)
    var value:Option[T]=None
    Tool.reTry(3) {
      val result = client.getRow(request)
      val row = result.getRow()
      if (result.getRow !=null && !result.getRow.isEmpty){
        val dataMap = ("value"::"z4z"::Nil).map(k => k -> getColData(row.getColumn(k).map(_.getValue).head)).toMap
        val hasZ4z=dataMap.getOrElse("z4z",false).asInstanceOf[Boolean]
        val data=if(hasZ4z) dataMap("value").asInstanceOf[Array[Byte]].unz4z else dataMap("value").asInstanceOf[Array[Byte]]
        val obj=ReflectTool.getObjectByBytes(data)
        if(obj.isInstanceOf[T]){
          value=Some(obj.asInstanceOf[T])
        }
      }
    }
    value
  }
  def delCache(key: String) = {
    val rowChange = new RowDeleteChange(cacheTableName)
    val primaryKey = PrimaryKeyBuilder.createPrimaryKeyBuilder().addPrimaryKeyColumn("key", PrimaryKeyValue.fromString(key)).build()
    rowChange.setPrimaryKey(primaryKey)
    val request = new DeleteRowRequest()
    request.setRowChange(rowChange)
    val result = client.deleteRow(request)
    result.getConsumedCapacity().getCapacityUnit().getWriteCapacityUnit()
  }

  /**
    *批量查询
    *
    * @return
    */
  def getAll={
    val criteria = new RangeRowQueryCriteria(cacheTableName)
    val inclusiveStartKey =  PrimaryKeyBuilder.createPrimaryKeyBuilder().addPrimaryKeyColumn("key",PrimaryKeyValue.INF_MIN).build()
    val exclusiveEndKey = PrimaryKeyBuilder.createPrimaryKeyBuilder().addPrimaryKeyColumn("key",PrimaryKeyValue.INF_MAX).build()
    criteria.setInclusiveStartPrimaryKey(inclusiveStartKey)
    criteria.setExclusiveEndPrimaryKey(exclusiveEndKey)
    criteria.setMaxVersions(1)
    val request = new GetRangeRequest()
    request.setRangeRowQueryCriteria(criteria)
    client.getRange(request).getRows.toList
  }

  /**
    * 清理缓存里面的数据
    *
    * @return
    */
  def cleanCache()={
    val now = System.currentTimeMillis() / 1000
    //筛选数据
    val datas=getAll.map{v=>
      v.getColumn("key").headOption.map(v=>v.getValue.asString())
    }
    //删除数据
    datas.filter(_.isDefined).map(_.get).foreach{data=>
      println(data)
      Cache.delCache(data)
    }
    (datas.size,datas.size,0)
  }

  def getColData(v:ColumnValue):Object={
    if(v==null) return null
    v.getType match{
      case ColumnType.BINARY =>v.asBinary()
      case  ColumnType.BOOLEAN =>Boolean.box(v.asBoolean())
      case  ColumnType.DOUBLE=>Double.box(v.asDouble())
      case ColumnType.INTEGER => Long.box(v.asLong())
      case ColumnType.STRING => v.asString()
    }
  }

}






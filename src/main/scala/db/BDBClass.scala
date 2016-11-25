package db

import java.util.{Date, UUID}

import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import tools._
import common._
import Tool._
/**
 * Created by Administrator on 15-6-25.
 */
class BaseBDBEntity[+Self <: BaseBDBEntity[Self]](tableName: String) extends BDBEntity(tableName) {
  def toJson: String = {
    BaseBDBEntity.map.writeValueAsString(this)
  }

  def fromJson(json: String): Self = {
    BaseBDBEntity.map.readValue(json, this.getClass).asInstanceOf[Self]
  }

  //将对应的更新类转为实体类
  def changeUpdateBean(): Self = {
    fromJson(toJson)
  }

  override def queryById(id: String, fields: String*): Option[Self] = {
    super.queryById(id,fields:_*) map (_.asInstanceOf[Self])
  }
  override def queryByIds(idName: String, ids: List[Long], fields: String*): List[Self] = {
    super.queryByIds(idName,ids,fields:_*) map (_.asInstanceOf[Self])
  }

  //这个接口需要传条件、排序
  override def queryPage(where: String, pageNum: Int, pageSize: Int, fields: String*):List[Self] = {
    val  list = super.queryPage(where, pageNum, pageSize,fields: _*)
     list map (_.asInstanceOf[Self])
  }

}

object BaseBDBEntity {
  private val map = new ObjectMapper() with ScalaObjectMapper
  map.registerModule(DefaultScalaModule)
  map.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
}

//存储文本记录
class LoanData(val id:String=UUID.randomUUID().toString,val Title:String="",val text:String="",val createTime:Date=new Date())extends BaseBDBEntity[LoanData]("LoanData")

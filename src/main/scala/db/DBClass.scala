package db

import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import common._
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
import scala.collection.mutable.HashMap
import common.TimeTool._


/**
  * Created by 林 on 14-3-26.
  */

class BaseDBEntity[+Self <: BaseDBEntity[Self]](tableName: String) extends DBEntity(tableName) {

  def uuid = UUID.randomUUID().toString.replace("-", "")

  def toJson: String = {
    BaseDBEntity.map.writeValueAsString(this)
  }

  def toMap: Map[String, Any] = {
    BaseDBEntity.map.readValue(toJson, Map[String, Any]().getClass).asInstanceOf[Map[String, Any]]
  }

  def toHashMap: HashMap[String, Any] = {
    BaseDBEntity.map.readValue(toJson, HashMap[String, Any]().getClass).asInstanceOf[HashMap[String, Any]]
  }

  def fromJson(json: String): Self = {
    BaseDBEntity.map.readValue(json, this.getClass).asInstanceOf[Self]
  }

  //将对应的更新类转为实体类
  def changeUpdateBean(): Self = {
    fromJson(toJson)
  }

  override def queryById(id: String): Option[Self] = {
    super.queryById(id) map (_.asInstanceOf[Self])
  }

  override def queryByIds(ids: List[String]): List[Self] = {
    super.queryByIds(ids) map (_.asInstanceOf[Self])
  }


  override def queryOne(sql: String, param: String*): Option[Self] = {
    super.queryOne(s"select * from $tableName where " + sql, param: _*) map (_.asInstanceOf[Self])
  }

  override def queryAll(): List[Self] = {
    super.queryAll map (_.asInstanceOf[Self])
  }

  override def query(where: String, param: String*): List[Self] = {
    super.query(where, param: _*) map (_.asInstanceOf[Self])
  }

  //这个接口需要传条件、排序
  override def queryPage(where: String, pageNum: Int, pageSize: Int, order: String, param: String*): (Int, List[Self]) = {
    val (count, list) = super.queryPage(where, pageNum, pageSize, order, param: _*)
    (count, list map (_.asInstanceOf[Self]))
  }

}

object BaseDBEntity {
  protected val map = new ObjectMapper() with ScalaObjectMapper
  map.registerModule(DefaultScalaModule)
  map.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
  map.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm"))


  def toJson(data: AnyRef) = {
    map.writeValueAsString(data)
  }
  def toHashMap(dbe:DBEntity): HashMap[String, Any]=BaseDBEntity.map.readValue(map.writeValueAsString(dbe), HashMap[String, Any]().getClass).asInstanceOf[HashMap[String, Any]]

  //自动检查数据的查询结果是否存在
  implicit class DBOptionAdd[T <: DBEntity](o: Option[T]) {
    def dbCheck: T = if (o.isEmpty) throw new DataNoFindExcepiton else o.get
  }

  def uuid = UUID.randomUUID().toString.replace("-", "")

}

import BaseDBEntity.uuid


/**
  * 注释说明   表级别
  *{"method":"get,post,put,delete"(管理后台中的方法),"ref":"quick" or "cache"(获取关联表的方式),"map":["lat","lng"](地图属性的经纬度)}
  *
  */


/**
  *系统设置表，系统中重要设置全部存储于此表中
  *
  */
//系统参数
class Setting(val id: Int = -1, val name: String = "", val value: String = "",val remark:String="") extends BaseDBEntity[Setting]("Setting")


/**
  * 用户
  *
  */
@ApiModel(value = "User",description = "用户基础属性")
class User(val id: Int = 0,
           @(ApiModelProperty @field)(value = "名字", required = true)
           val name: String = "",
           @(ApiModelProperty @field)(value = "手机号", required = false)
           val phone: String = "",
           @(ApiModelProperty @field)(value = "邮箱", required = false)
           val email:String="",
           @(ApiModelProperty @field)(value = "密码", required = true)
           val pwd:String="",
           @(ApiModelProperty @field)(value = "等级", required = false)
           val level:Int=0,
           @(ApiModelProperty @field)(value = "状态(0=正常,1=未启用,2=封禁)", required = false)
           val status: Int = -1,
           @(ApiModelProperty @field)(value = "扩展数据,用来存一些莫名奇妙的数据", required = false,hidden = true)
           val ext: String = "",
           @(ApiModelProperty @field)(value = "创建时间(前端请忽略)", required = false,hidden = true)
           val createTime: Date = new Date(System.currentTimeMillis())) extends BaseDBEntity[User]("User")

@ApiModel(value = "UserAccount",description = "用户账号")
class UserAccount(val id:Int=0,
                  @(ApiModelProperty @field)(value = "用户", required = true,reference = "User")
                  val uid:Int=0,
                  @(ApiModelProperty @field)(value = "平台(ppd)", required = true)
                  val platform:String="",
                  @(ApiModelProperty @field)(value = "用户名", required = true)
                  val userName:String="",
                  @(ApiModelProperty @field)(value = "密码", required = true)
                  val passWord:String="",
                  @(ApiModelProperty @field)(value = "账户余额", required = true)
                  val money:BigDecimal=BigDecimal(0),
                  @(ApiModelProperty @field)(value = "账户总额", required = true)
                  val allMoney:BigDecimal=BigDecimal(0),
                  @(ApiModelProperty @field)(value = "创建时间", required = true)
                  val createTime: Date = new Date(System.currentTimeMillis())) extends BaseDBEntity[UserAccount]("UserAccount")

@ApiModel(value = "Loan",description = "借款信息")
class Loan(val id:Int=0,
           @(ApiModelProperty @field)(value = "标题", required = true)
           val Title:String="",
           @(ApiModelProperty @field)(value = "id", required = true)
           val ListingId:Int=0,
           @(ApiModelProperty @field)(value = "金额", required = true)
           val Amount:BigDecimal=0,
           @(ApiModelProperty @field)(value = "分级", required = true)
           val CreditCode:String="",
           @(ApiModelProperty @field)(value = "完成度", required = true)
           val Funding:Int=0,
           @(ApiModelProperty @field)(value = "不知道干嘛的", required = true)
           val MonthlyPayment:Int=0,
           @(ApiModelProperty @field)(value = "借款期限", required = true)
           val Months:Int=0,
           @(ApiModelProperty @field)(value = "利率", required = true)
           val Rate:Double=0,
           @(ApiModelProperty @field)(value = "扩展数据", required = false,hidden = true)
           val ext:String="",
           @(ApiModelProperty @field)(value = "创建日期", required = false,hidden = true)
           val createTime:Date=new Date(),
           @(ApiModelProperty @field)(value = "最后更新日期", required = false,hidden = true)
           val lastUpdate:Date=null
          )extends BaseDBEntity[Loan]("Loan")

@ApiModel(value = "LoanInfo",description = "借款详细信息")
class LoanInfo(val id:Int=0,
               @(ApiModelProperty @field)(value = "借款用户", required = true,reference = "LoanUser")
               val uid:String="",
               @(ApiModelProperty @field)(value = "标题", required = true)
               val Title:String="",
               @(ApiModelProperty @field)(value = "id", required = true)
               val ListingId:Int=0,
               @(ApiModelProperty @field)(value = "借款信息", required = true)
               val info:String="",
               @(ApiModelProperty @field)(value = "认证信息", required = true)
               val very:String="",
               @(ApiModelProperty @field)(value = "审核信息", required = true)
               val audit:String="",
               @(ApiModelProperty @field)(value = "统计信息", required = true)
               val count:String="",
               @(ApiModelProperty @field)(value = "扩展数据", required = false,hidden = true)
               val ext:String="",
               @(ApiModelProperty @field)(value = "创建日期", required = false,hidden = true)
               val createTime:Date=new Date()
              )extends BaseDBEntity[LoanInfo]("LoanInfo")

@ApiModel(value = "LoanUser",description = "借款用户")
class LoanUser(val id: String ="",
           @(ApiModelProperty @field)(value = "名字", required = true)
           val name: String = "",
           @(ApiModelProperty @field)(value = "性别", required = false)
           val sex: String = "",
           @(ApiModelProperty @field)(value = "注册时间", required = false)
           val regTime:String="",
           @(ApiModelProperty @field)(value = "身份认证", required = true)
           val idv:String="",
           @(ApiModelProperty @field)(value = "视频认证", required = true)
           val ivv:String="",
           @(ApiModelProperty @field)(value = "学历认证", required = true)
           val iev:String="",
           @(ApiModelProperty @field)(value = "手机认证", required = true)
           val ipv:String="",
           @(ApiModelProperty @field)(value = "等级分", required = false)
           val level:Int=0,
           @(ApiModelProperty @field)(value = "状态(0=正常,1=未启用,2=封禁)", required = false)
           val status: Int = 0,
           @(ApiModelProperty @field)(value = "扩展数据,用来存一些莫名奇妙的数据", required = false,hidden = true)
           val ext: String = "",
           @(ApiModelProperty @field)(value = "创建时间(前端请忽略)", required = false,hidden = true)
           val createTime: Date = new Date(System.currentTimeMillis())) extends BaseDBEntity[LoanUser]("LoanUser")

@ApiModel(value = "LoanText",description = "借款html")
class LoanText(val id:Int=0,
           @(ApiModelProperty @field)(value = "标题", required = true)
           val Title:String="",
           @(ApiModelProperty @field)(value = "id", required = true)
           val ListingId:Int=0,
           @(ApiModelProperty @field)(value = "原始数据", required = true)
           val text:String="",
           @(ApiModelProperty @field)(value = "创建日期", required = false,hidden = true)
           val createTime:Date=new Date()
          )extends BaseDBEntity[LoanText]("LoanText")
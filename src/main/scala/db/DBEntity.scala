package db

import java.security.Key
import java.sql.{Connection, SQLException, Statement}
import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory
import common.Tool._
import common._
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import org.apache.commons.dbcp.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.{MapListHandler, ScalarHandler}
import tools.ReflectTool.getConstructorParamNames

import scala.annotation.meta.field
import scala.collection.JavaConversions._
import scala.collection.mutable

class DBEntity(@(ApiModelProperty @field)(hidden= true)val tableName: String) {
  private lazy val created = getConstructorParamNames(this.getClass())
  private lazy val methods = this.getClass().getMethods() filter {
    m => m.getParameterTypes().length == 0
  }

  private def getFiledValue(key: String) = {
    methods.filter(_.getName() == key)(0).invoke(this)
  }

  def getFieldKeyValues(fields: String*) = {
    (if (fields.size == 0) {
      getConstructorParamNames(this.getClass()).maxBy(_._2.length)._2 map (_._1)
    } else {
      fields.toList
    }) map (f => f -> methods.filter(_.getName() == f)(0).invoke(this)) filter (_._2 != null) map (kv => kv._1 -> (kv._2 match {
      case b: java.lang.Boolean => if (b) 1 else 0
      case s: String => s
      case d: Date => DBEntity.getFormatStringByDate(d)
      case bd: BigDecimal => bd.toDouble
      case o: Any => o
    }))
  }

  def getFieldKeyTypes(fields: String*) = {
    val keys = (if (fields.size == 0) {
      getConstructorParamNames(this.getClass()).maxBy(_._2.length)._2 map (_._1)
    } else {
      fields.toList
    })
    keys map (f => f -> methods.filter(_.getName() == f)(0).invoke(this)) filter (_._2 != null) map (kv => kv._1 -> (kv._2 match {
      case b: java.lang.Boolean => "BIT"
      case s: String => "VARCHAR(255)"
      case i: java.lang.Integer => "INT"
      case l: java.lang.Long => "BIGINT"
      case d: java.lang.Double => "DOUBLE"
      case d: Date => "TIMESTAMP"
      case bd: BigDecimal => "DECIMAL(15,2)"
      case o: Any => "VARCHAR(255)"
    }))
  }

  private def getInstallSqlAndparams(withId: Boolean, fields: String*) = {
    val fieldKVs = getFieldKeyValues(fields: _*)
    val realFieldKVs = if (withId) fieldKVs else fieldKVs filterNot (_._1 == "id")
    ((s"insert INTO $tableName (${realFieldKVs map (_._1) mkString (",")}) values (${realFieldKVs map (v => if (v._2.isInstanceOf[String]) "?" else v._2) mkString (",")})") ->
      (realFieldKVs.filter(_._2.isInstanceOf[String]).map(_._2.asInstanceOf[String])))
  }

  /**
    * 构造表创建sql
    *
    * @return sql
    */
  private def getCreateSql() = {
    val fieldKVs = getFieldKeyTypes()
    val hasId = (fieldKVs filter (_._1 == "id") size) > 0;
    if (hasId) {
      val IdKVs = fieldKVs map (kv => if (kv._1 == "id") kv._1 -> (kv._2 + " AUTO_INCREMENT ") else kv)
      s"CREATE TABLE $tableName (${IdKVs map (kv => kv._1 + " " + kv._2) mkString (",")},PRIMARY KEY (id)) "
    } else {
      s"CREATE TABLE $tableName (${fieldKVs map (kv => kv._1 + " " + kv._2) mkString (",")}) "
    }
  }

  /**
    * 数据库表
    */
  def createTable() {
    DBEntity.sql(getCreateSql)
  }

  def createTableSql() = {
    getCreateSql
  }

  def deleteTable(safe: Boolean = true) {
    DBEntity.sql(s"DROP TABLE ${if (safe) "IF EXISTS" else ""} $tableName")
  }

  def cleanTable() = {
    DBEntity.sql(s"delete from ${tableName}")
  }

  private def getInstallSqlWithUpdate(field: String, fields: String*) = {
    val (sql, params) = getInstallSqlAndparams(false, fields: _*)
    ((sql + s" ON DUPLICATE KEY UPDATE $field =?") -> (params ::: (getFiledValue(field).toString) :: Nil))
  }

  private def getDeleteIdSql(id: Int) = {
    s"delete $tableName where id=$id"
  }

  def update(where: String, fields: String*) = {
    val whereValue = getFiledValue(where).toString
    val kvs = getFieldKeyValues(fields: _*)
    val params = kvs filter (_._2.isInstanceOf[String]) map (_._2.asInstanceOf[String])
    DBEntity.sql(s"update $tableName set ${kvs map (kv => if (kv._2.isInstanceOf[String]) kv._1 + "=?" else kv._1 + "=" + kv._2 + "") mkString (",")} where $where =?", (params ::: whereValue :: Nil): _*)
  }

  def updateNoEmptyById() = {
    val m = getFieldKeyValues() filter (v => v._2 != null && !DBEntity.isEmpty(v._2))
    val updates = (m map (_._1) toList)
    update("id", updates: _*)
  }

  def insert(fields: String*) = {
    DBEntity.insert(this, false, fields: _*)
  }

  def insertWithId(fields: String*) = {
    DBEntity.insert(this, true, fields: _*)
  }

  def insertUpdate(updateField: String, fields: String*) {
    DBEntity.insertUpdate(this, updateField, fields: _*)
  }

  def delete(where: String) {
    DBEntity.delete(this, where)
  }

  def queryById(id: String): Option[_ <: DBEntity] = {
    val l = DBEntity.apply(this.getClass(), s"select * from $tableName where id=?",id)
    if (l.length == 0) {
      None
    } else {
      Some(l(0).asInstanceOf[DBEntity])
    }
  }

  def queryOne(sql: String, param: String*): Option[_ <: DBEntity] = {
    val l = DBEntity.apply(this.getClass(), sql, param: _*)
    if (l.length == 0) {
      None
    } else {
      Some(l(0))
    }
  }

  def queryMap(sql: String, param: String*): List[Map[String, Object]] = {
    DBTool.queryDataMap(sql, param: _*) map (_.toMap) toList
  }

  def quickById(id: String): Option[_ <: DBEntity] = {
    val key = "dbcache_" + tableName
    val cache = Cache.getCache(key+id)
    if (cache.isEmpty) {
      val data = queryById(id)
      if (!data.isEmpty) {
        Cache.setCache(key+id,data.get)
      }
      data
    } else cache.asInstanceOf[Option[DBEntity]]
  }

  def queryByIds(ids: List[String]): List[_ <: DBEntity] = {
    DBEntity.queryByIds(getClass, tableName, ids)
  }

  def queryAll(): List[_ <: DBEntity] = {
    DBEntity.apply(getClass, s"select * from $tableName ")
  }

  //这个接口只管where之后的东西，还有参数
  def query(where: String, param: String*): List[_ <: DBEntity] = {
    val realWhere = if (where.trim.isEmpty) "1=1" else where
    DBEntity.apply(getClass, s"select * from $tableName where " + realWhere, param: _*)
  }

  def queryCount(where: String, param: String*): Int = {
    val realWhere = if (where.trim.isEmpty) "1=1" else where
    DBTool.count(s"select count(*) from $tableName where " + realWhere, param: _*)
  }

  //这个接口需要传条件、排序
  def queryPage(where: String, pageNum: Int, pageSize: Int, order: String, param: String*): (Int, List[_ <: DBEntity]) = {
    val realPageNum = if (pageNum < 1) 1 else pageNum
    val realPageSize = if (pageSize < 1) 1 else pageSize
    val realWhere = if (where.trim.isEmpty) "1=1" else where
    val realOrder = if (order.trim.isEmpty) "" else " order by " + order
    (queryCount(where, param: _*), query(realWhere + realOrder + s" LIMIT ${(realPageNum - 1) * pageSize},$realPageSize ", param: _*))
  }


  def tableInfo ={
    val clazz=this.getClass
    val model=clazz.getAnnotations.filter(_.isInstanceOf[ApiModel]).map(_.asInstanceOf[ApiModel]).headOption
    val params=clazz.getDeclaredFields.map(v=>v.getName -> v.getDeclaredAnnotations.filter(_.isInstanceOf[ApiModelProperty]).map(_.asInstanceOf[ApiModelProperty]).headOption).map{np=>
      if(np._2.isEmpty){
        Map("name" ->np._1 ,"value"->np._1,"remark"->"","required" ->false ,"reference"->"","hidden"->false)
      }else{
        Map("name" ->np._1 , "value"->StringTool.getNoExp(np._2.get.value(),"(",")"),"remark"->StringTool.getExpBody(np._2.get.value(),"(",")"),"required" ->np._2.get.required() ,"reference"->np._2.get.reference(),"hidden"->np._2.get.hidden())
      }
    }
    (model,params)
  }

}

object DBEntity {
  lazy val conf = ConfigFactory.load()

  def getDataSouorce() = {
    val ds = new BasicDataSource()
    ds.setDriverClassName(conf.getString("mysql.driver"))
    ds.setUsername(conf.getString("mysql.username"))
    ds.setPassword(conf.getString("mysql.password"))
    ds.setUrl(conf.getString("mysql.url"))
    ds.setValidationQuery("select 1")
    ds
  }

  //开始事务
  def startTransaction() = {
    DBTool.startTransaction()
  }

  //结束事务
  def endTransaction(rockback: Boolean = false, id: String = null) {
    if (id != null) {
      DBTool.endTransaction(rockback, id)
    } else {
      DBTool.endTransaction(rockback)
    }

  }

  /*从map 构造一个实例*/
  def apply[T](clazz: Class[_ <: T], map: Map[String, Object]): T = {
    val created = getConstructorParamNames(clazz).maxBy(_._2.length)
    val params = created._2 map {
      name_type =>
        val value = map.getOrElse(name_type._1, null)
        val t = name_type._2
        if (null != value && (value.getClass().isInstance(t) || value.getClass() == t)) {
          value
        } else {
          t.getName match {
            case "java.sql.Date" => if (value == null) null else new java.sql.Date(value.asInstanceOf[java.util.Date].getTime())
            case "java.sql.Time" => if (value == null) null else new java.sql.Time(value.asInstanceOf[java.util.Date].getTime())
            case "java.sql.Timestamp" => if (value == null) null else new java.sql.Timestamp(value.asInstanceOf[java.util.Date].getTime())
            case "java.lang.String" => if (value == null) null else value.asInstanceOf[String]
            case "scala.math.BigDecimal" => if (value == null) null else BigDecimal(value.toString)
            case "boolean" => if (value == null) null else if (value.isInstanceOf[Boolean]) value else if (value.isInstanceOf[String]) Boolean.box(value.toString().toLowerCase == "true") else Boolean.box(value.asInstanceOf[Int] == 1)
            case _ => value
          }
        }
    }
    //    params foreach (v=> if(v==null) print("null") else print(v.getClass() + ":"+v))
    created._1.newInstance(params: _*).asInstanceOf[T]
  }

  /*从sql构造一个列表*/
  def apply[T](clazz: Class[_ <: T], sql: String, param: String*): List[T] = {
    DBTool.queryDataMap(sql, param: _*) map {
      m => apply[T](clazz, m.toMap)
    } toList
  }

  def query[T](clazz: Class[_ <: T], sql: String, param: String*): List[T] = {
    apply[T](clazz, sql, param: _*)
  }

  def queryAll[T](clazz: Class[_ <: T], table: String): List[T] = {
    apply[T](clazz, s"select * from $table ")
  }

  def queryPage[T](clazz: Class[_ <: T], sql: String, pageNum: Int, pageSize: Int, param: String*): List[T] = {
    val realPageNum = if (pageNum < 0) 0 else pageNum
    apply[T](clazz, sql + s" LIMIT ${realPageNum * pageSize},$pageSize ", param: _*)
  }

  def queryOne[T](clazz: Class[_ <: T], sql: String, param: String*): Option[T] = {
    val l = apply[T](clazz, sql, param: _*)
    if (l.length == 0) {
      None
    } else {
      Some(l(0))
    }
  }

  def queryById[T](clazz: Class[_ <: T], tableName: String, id: Long): Option[T] = {
    val l = apply[T](clazz, s"select * from $tableName where id=$id")
    if (l.length == 0) {
      None
    } else {
      Some(l(0))
    }
  }

  /*插入数据，默认以最长的构造函数的参数作为插入字段*/
  def insert(entity: DBEntity, withId: Boolean, fields: String*) = {
    val (sql, params) = entity.getInstallSqlAndparams(withId, fields: _*)
    val id = DBTool.insert(sql, params: _*)
    id
  }

  /*插入数据重复后更新字段*/
  def insertUpdate(entity: DBEntity, updateField: String, fields: String*) {
    val (sql, params) = entity.getInstallSqlWithUpdate(updateField, fields: _*)
    DBTool.update(sql, params: _*)
  }

  def delete(entity: DBEntity, where: String) {
    val whereValue = entity.getFiledValue(where).toString
    DBTool.update(s"delete from ${entity.tableName} where $where = ?", whereValue)
  }

  def sql(sql: String, params: String*) = {
    DBTool.update(sql, params: _*)
  }
  def updateBatch(sql: List[String]): List[Int] = {
    DBTool.updateBatch(sql)
  }

  def queryMap(sql: String, params: AnyRef*) = {
    DBTool.queryDataMap(sql, params: _*) map (_.toMap)
  }

  def queryByIds[T <: DBEntity](clazz: Class[T], table: String, ids: List[String]): List[T] = {
    if (ids.isEmpty) Nil
    else apply(clazz, s"select * from $table where id in (${ids.map("'"+_+"'").mkString(",")})")
  }

  //事务方法域
  def transaction(fun: => Any)(exfun: Throwable => Any) {
    try {
      val id = DBEntity.startTransaction()
      fun
      DBEntity.endTransaction(false, id)
    } catch {
      case t: Throwable =>
        safe {
          DBEntity.endTransaction(true)
        }
        safe {
          exfun(t)
        }
    }
  }

  def isEmpty(str: String) = {
    (null == str || str.isEmpty)
  }

  def isEmpty(bean: Any): Boolean = {
    bean match {
      case s: String => isEmpty(bean.asInstanceOf[String])
      case i: Int => bean.asInstanceOf[Int] == -1
      case d: Double => bean.asInstanceOf[Double] == -1
      case d: Boolean => !bean.asInstanceOf[Boolean]
      case b: BigDecimal => b == null || b.asInstanceOf[BigDecimal] == -1
      case _ => bean == null
    }
  }

  def getFormatStringByDate(date: Date): String = {
    if (null == date) {
      null
    } else {
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
    }
  }
  def count(sql:String,params:String*)={
    DBTool.count(sql,params:_*)
  }
}

private object DBTool {
  lazy val dataSource: BasicDataSource = DBEntity.getDataSouorce()
  private val transactionMap = new mutable.HashMap[String, Connection]()

  //获取连接
  def getConn() = {
    transactionMap.getOrElse(Thread.currentThread().getId.toString, dataSource.getConnection)
  }

  //返回连接
  def returnConn(conn: Connection) = {
    if (!transactionMap.contains(Thread.currentThread().getId.toString)) {
      conn.close()
    }
  }

  //开始事务
  def startTransaction() = {
    val conn = dataSource.getConnection
    conn.setAutoCommit(false)
    transactionMap.put(Thread.currentThread().getId.toString, conn)
    Thread.currentThread().getId.toString
  }

  //结束事务
  def endTransaction(rockback: Boolean = false, id: String = Thread.currentThread().getId.toString) {
    val conn = transactionMap.get(id).get
    try {
      transactionMap.remove(id)
      if (rockback) {
        conn.rollback()
      } else {
        conn.commit()
      }
    } finally {
      conn.close()
    }
  }

  def insert(sql: String, params: Any*) = {
    val conn = getConn()
    val st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    if (null != params && params.length > 0) {
      for (i <- 0 until params.length) {
        st.setString(i + 1, params(i).toString())
      }
    }
    try {
      st.executeUpdate()
      val rs = st.getGeneratedKeys()
      if (rs.next()) rs.getInt(1) else -1
    } catch {
      case e: SQLException => throw new DetailSQLException(e, sql)
    } finally {
      st.close()
      returnConn(conn)
    }
  }

  def update(sql: String, params: Any*): Int = {
    val conn = getConn()
    val st = conn.prepareStatement(sql)
    if (null != params && params.length > 0) {
      for (i <- 0 until params.length) {
        st.setString(i + 1, params(i).toString())
      }
    }
    try {
      if (st.execute()) {
        st.getUpdateCount()
      } else {
        0
      }
    } catch {
      case e: SQLException => throw new DetailSQLException(e, sql)
    } finally {
      st.close()
      returnConn(conn)
    }
  }
  def updateBatch(sql: List[String]): List[Int] = {
    val conn = getConn()
    val st = conn.createStatement()
    sql.foreach(v=>st.addBatch(v))
    try {
      st.executeBatch().toList
    } catch {
      case e: SQLException => throw new DetailSQLException(e, sql.mkString(","))
    } finally {
      st.close()
      returnConn(conn)
    }
  }

  def queryDataMap(sql: String, params: AnyRef*) = {
    val conn = getConn()
    try {
      new QueryRunner().query(conn, sql, new MapListHandler(), params: _*)
    } catch {
      case e: SQLException => throw new DetailSQLException(e, sql)
    } finally {
      returnConn(conn)
    }
  }

  def count(sql: String, params: AnyRef*) = {
    getAnAttr(sql, params: _*).toString.toInt
  }

  /**
    * 获得第一个查询第一行第一列
    *
    * @param sql
    * @param params
    * @return
    */
  def getAnAttr(sql: String, params: AnyRef*) = {
    val conn = getConn()
    try {
      new QueryRunner().query(conn, sql, new ScalarHandler[Long](1), params: _*)
    } catch {
      case e: SQLException => throw new DetailSQLException(e, sql)
      case ex: Exception => throw ex
    } finally {
      returnConn(conn)
    }
  }
}
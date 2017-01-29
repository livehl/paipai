package db
import java.util.Date

import com.typesafe.config.ConfigFactory
import common._
import Tool._
import com.alicloud.openservices.tablestore.SyncClient
import com.alicloud.openservices.tablestore.model._
import tools.ReflectTool.getConstructorParamNames
import tools.Z4ZTool._

import scala.collection.JavaConversions._

/**
  * nosql 数据库引擎
  * @param tableName
  */
class BDBEntity(val tableName: String){
  private lazy val created = getConstructorParamNames(this.getClass())
  private lazy val methods = this.getClass().getMethods() filter {
    m => !m.getName().contains("_") && m.getParameterTypes().length == 0
  }

  private def getFiledValue(key: String) = {
    methods.filter(_.getName() == key)(0).invoke(this)
  }

  private def getBDBFiledValue(key: String) = {
    val v = getFiledValue(key)
    v match {
      case null => null
      case b: java.lang.Boolean => ColumnValue.fromBoolean(b)
      case i: java.lang.Integer => ColumnValue.fromLong(i.toLong)
      case d: java.lang.Double => ColumnValue.fromDouble(d)
      case f: java.lang.Float => ColumnValue.fromDouble(f.toDouble)
      case l: java.lang.Long => ColumnValue.fromLong(l)
      case s: String => if(BDBEntity.useZ4z) ColumnValue.fromBinary(s.z4z) else ColumnValue.fromString(s)
      case d: Date => ColumnValue.fromLong(d.getTime)
      case bd: BigDecimal => ColumnValue.fromDouble(bd.toDouble)
      case b: Array[Byte] => ColumnValue.fromBinary(b)
      case o: Any =>throw new VenusException("不支持的数据结构:"+o.getClass.getName)
    }
  }

  private def getBDBKeyFiledValue(key: String) = {
    val v = getFiledValue(key)
    v match {
      case null => null
      case i: Integer => PrimaryKeyValue.fromLong(i.toLong)
      case l: java.lang.Long => PrimaryKeyValue.fromLong(l)
      case s: String => PrimaryKeyValue.fromString(s)
    }
  }

    private def getBDBKeyValue(key: AnyRef) = {
      key match {
        case null => null
        case i: Integer => PrimaryKeyValue.fromLong(i.toLong)
        case l: java.lang.Long => PrimaryKeyValue.fromLong(l)
        case s: String => PrimaryKeyValue.fromString(s)
        case b: Array[Byte] => PrimaryKeyValue.fromBinary(b)
        case o:AnyRef =>throw new VenusException("不支持的数据结构:"+o.getClass.getName)
      }
    }

  def getFieldKeys(fields: String*) = {
    (if (fields.size == 0) {
      getConstructorParamNames(this.getClass()).maxBy(_._2.length)._2 map (_._1)
    } else {
      fields.toList
    })
  }

  def getFieldKeyValues(fields: String*) = {
    (if (fields.size == 0) {
      getConstructorParamNames(this.getClass()).maxBy(_._2.length)._2 map (_._1)
    } else {
      fields.toList
    }) map (f => f -> methods.filter(_.getName() == f)(0).invoke(this)) filter (_._2 != null) map (kv => kv._1 -> (kv._2 match {
      case b: java.lang.Boolean => b
      case s: String => s
      case d: Date => d.getTime
      case bd: BigDecimal => bd.toDouble
      case o: Any => o
    }))
  }

  /**
   * 数据库表
   */
  def createTable() {
    val tableMeta = new TableMeta(tableName)
    val primaryKeyType=getFiledValue("id") match{
      case _:String=>PrimaryKeyType.STRING
      case _:java.lang.Long=>PrimaryKeyType.INTEGER
      case _:java.lang.Integer=>PrimaryKeyType.INTEGER
      case _:Any=> throw new VenusException("不支持的id类型")
    }
    tableMeta.addPrimaryKeyColumn("id",primaryKeyType)
    // 将该表的读写CU都设置为100
    val ttl=if(BDBEntity.conf.hasPath("ots.ttl")) BDBEntity.conf.getInt("ots.ttl") else -1
    val request = new CreateTableRequest(tableMeta, new TableOptions(ttl, 1))
    request.setTableMeta(tableMeta)
    request.setReservedThroughput(new ReservedThroughput(0, 0))
    BDBEntity.client.createTable(request)
  }

  def deleteTable() {
    val request = new DeleteTableRequest(tableName)
    BDBEntity.client.deleteTable(request)
  }

  def update(where: String, fields: String*) = {
    val rowChange = new RowUpdateChange(tableName)
    val primaryKeys = PrimaryKeyBuilder.createPrimaryKeyBuilder()
    val whereValue = getBDBKeyFiledValue(where)
    if (whereValue == null) throw new EmptyFieldExcepiton
    primaryKeys.addPrimaryKeyColumn(where, whereValue)
    rowChange.setPrimaryKey(primaryKeys.build())
    getFieldKeys(fields: _*).foreach { k =>
      val v = getBDBFiledValue(k)
      if (v == null) rowChange.deleteColumns(k)
      else
        rowChange.put(k,v)
    }
    rowChange.setCondition(new Condition(RowExistenceExpectation.IGNORE))
    val request = new UpdateRowRequest()
    request.setRowChange(rowChange)
    reTry(5){
      val result = BDBEntity.client.updateRow(request)
      result.getConsumedCapacity.getCapacityUnit.getWriteCapacityUnit
    }
  }

  def updateNoEmptyById() = {
    val m = getFieldKeyValues() filter (v => v._2 != null && !DBEntity.isEmpty(v._2))
    val updates = (m map (_._1) filter (_ != "id") toList)
    update("id", updates: _*)
  }

  def insert(fields: String*) = {
    val request = new PutRowRequest()
    request.setRowChange(getInstallRowChange(fields:_*))
    reTry(3) {
      val result = BDBEntity.client.putRow(request)
      result.getConsumedCapacity().getCapacityUnit().getWriteCapacityUnit()
    }
  }

  private def getInstallRowChange(fields: String*)={
    val rowChange = new RowPutChange(tableName)
    val primaryKey = PrimaryKeyBuilder.createPrimaryKeyBuilder()
    val idValue = getBDBKeyFiledValue("id")
    if (idValue == null) throw new EmptyFieldExcepiton
    primaryKey.addPrimaryKeyColumn("id", idValue)
    rowChange.setPrimaryKey(primaryKey.build())
    getFieldKeys(fields: _*).filter(_ != "tableName").filter(_ != "id").foreach { k =>
      val v = getBDBFiledValue(k)
      if (v != null) rowChange.addColumn(k, v)
    }
    rowChange.setCondition(new Condition(RowExistenceExpectation.IGNORE))
    rowChange
  }

  def insertUpdate(updateField: String, fields: String*) {
    val rowChange = new RowUpdateChange(tableName)
    val primaryKey =PrimaryKeyBuilder.createPrimaryKeyBuilder()
    val idValue = getBDBKeyFiledValue(updateField)
    if (idValue == null) throw new EmptyFieldExcepiton
    primaryKey.addPrimaryKeyColumn(updateField, idValue)
    rowChange.setPrimaryKey(primaryKey.build())
    getFieldKeys(fields: _*).filter(_ != updateField).foreach { k =>
      val v = getBDBFiledValue(k)
      if (v != null) rowChange.put(k, v)
    }
    rowChange.setCondition(new Condition(RowExistenceExpectation.IGNORE))
    val request = new UpdateRowRequest()
    request.setRowChange(rowChange)
    val result = BDBEntity.client.updateRow(request)
    result.getConsumedCapacity().getCapacityUnit().getWriteCapacityUnit()
  }

  def delete(where: String) {
    val rowChange = new RowDeleteChange(tableName)
    val primaryKeys = PrimaryKeyBuilder.createPrimaryKeyBuilder()
    val idValue = getBDBKeyFiledValue(where)
    if (idValue == null) throw new EmptyFieldExcepiton
    primaryKeys.addPrimaryKeyColumn(where, idValue)
    rowChange.setPrimaryKey(primaryKeys.build())
    val request = new DeleteRowRequest()
    request.setRowChange(rowChange)
    val result = BDBEntity.client.deleteRow(request)
    result.getConsumedCapacity().getCapacityUnit().getWriteCapacityUnit()
  }

  def queryById(id: String, fields: String*): Option[_ <: BDBEntity] = {
    val criteria = new SingleRowQueryCriteria(tableName)
    val primaryKeys =PrimaryKeyBuilder.createPrimaryKeyBuilder()
    val idValue = getBDBKeyValue(id)
    primaryKeys.addPrimaryKeyColumn("id", idValue)
    criteria.setPrimaryKey(primaryKeys.build())
    criteria.addColumnsToGet(getFieldKeys(fields:_*).filter(_ != "tableName").filter(_ != "id"))
    criteria.setMaxVersions(1)

    val request = new GetRowRequest()
    request.setRowQueryCriteria(criteria)
    var value:Option[_ <: BDBEntity]=None
    Tool.reTry(3) {
      val result = BDBEntity.client.getRow(request)
      val row = result.getRow()
      if (row ==null || row.getColumns.isEmpty)
        value=None
      else {
        val columns=row.getColumns.map(v=> v.getName->v.getValue).toMap
        val dataMap = getFieldKeys(fields: _*).filter(_ != "tableName").filter(_ != "id").toList.map(k => k -> getColData(columns(k))).toMap + ("id" ->id)
        value=Some(BDBEntity.apply(this.getClass(), dataMap))
      }
    }
    return value;
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

  def queryByIds(idName: String, ids: List[Long], fields: String*): List[_ <: BDBEntity] = {
    val request = new BatchGetRowRequest()
    val tableRows = new MultiRowQueryCriteria(tableName)
    ids.foreach { i =>
      val primaryKeys =PrimaryKeyBuilder.createPrimaryKeyBuilder()
        primaryKeys.addPrimaryKeyColumn(idName,
        PrimaryKeyValue.fromLong(i))
      tableRows.addRow(primaryKeys.build())
    }
    tableRows.addColumnsToGet(getFieldKeys(fields:_*))
    tableRows.setMaxVersions(1)
    request.addMultiRowQueryCriteria(tableRows)
    val result = BDBEntity.client.batchGetRow(request)
    val status = result.getTableToRowsResult
    status.values().map(v => v.filter(_.isSucceed)).flatten.map { v =>
      val columns=v.getRow.getColumns.map(c=> c.getName->c.getValue).toMap
      val dataMap = getFieldKeys(fields: _*).filter(_ != "tableName").filter(_ != "id").toList.map(k => k ->getColData(columns(k))).toMap + (idName -> getColData(columns(idName)))
      BDBEntity.apply(this.getClass(), dataMap)
    } toList
  }

  //范围查询
  def queryRange(id: String, start: Long, end: Long, fields: String*): List[_ <: BDBEntity] = {
    val criteria = new RangeRowQueryCriteria(tableName)
    val inclusiveStartKey = PrimaryKeyBuilder.createPrimaryKeyBuilder()
    inclusiveStartKey.addPrimaryKeyColumn(id, PrimaryKeyValue.fromLong(start))
    inclusiveStartKey.addPrimaryKeyColumn(id, PrimaryKeyValue.INF_MIN)
    // 范围的边界需要提供完整的PK，若查询的范围不涉及到某一列值的范围，则需要将该列设置为无穷大或者无穷小
    val exclusiveEndKey = PrimaryKeyBuilder.createPrimaryKeyBuilder()
    exclusiveEndKey.addPrimaryKeyColumn(id, PrimaryKeyValue.fromLong(end))
    exclusiveEndKey.addPrimaryKeyColumn(id, PrimaryKeyValue.INF_MAX)
    // 范围的边界需要提供完整的PK，若查询的范围不涉及到某一列值的范围，则需要将该列设置为无穷大或者无穷小
    criteria.setInclusiveStartPrimaryKey(inclusiveStartKey.build())
    criteria.setExclusiveEndPrimaryKey(exclusiveEndKey.build())
    criteria.setMaxVersions(1)
    val request = new GetRangeRequest()
    request.setRangeRowQueryCriteria(criteria)
    val result = BDBEntity.client.getRange(request)
    val rows = result.getRows()
    rows.map { v =>
      val columns=v.getColumns.map(v=> v.getName->v.getValue).toMap
      val dataMap = getFieldKeys(fields: _*).toList.map(k => k -> getColData(columns(k))).toMap + (id -> getColData(columns(id)))
      BDBEntity.apply(this.getClass(), dataMap)
    } toList
  }

  def queryCount(where: String, param: String*): Int = {
    throw new UnSupportExcepiton
  }

  def queryPage(where: String, pageNum: Int, pageSize: Int, fields: String*): List[_ <: BDBEntity] = {
    val realPageNum = if (pageNum < 1) 1 else pageNum
    val realPageSize = if (pageSize < 1) 1 else pageSize
    queryRange(where, realPageNum * realPageSize, realPageSize, fields: _*)
  }

}

object BDBEntity {
  private lazy val conf = ConfigFactory.load()
  private lazy val client = new SyncClient(conf.getString("ots.url"), conf.getString("ali.accessId"), conf.getString("ali.accessKey"), conf.getString("ots.instanceName"))
  private val useZ4z=conf.getBoolean("ots.z4z")
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
            case "java.util.Date" => if (value == null) null else  new Date(value.asInstanceOf[Long])
            case "java.sql.Time" => if (value == null) null else new java.sql.Time(value.asInstanceOf[java.util.Date].getTime())
            case "java.sql.Timestamp" => if (value == null) null else new java.sql.Timestamp(value.asInstanceOf[java.util.Date].getTime())
            case "java.lang.String" => if (value == null) null else if(value.isInstanceOf[Array[Byte]]) value.asInstanceOf[Array[Byte]].unz4zStr else value.asInstanceOf[String]
            case "scala.math.BigDecimal" => if (value == null) null else BigDecimal(value.toString)
            case "boolean" => if (value == null) null else if (value.isInstanceOf[Boolean]) value else Boolean.box(value.asInstanceOf[Int] == 1)
            case "int"=>value.toString.toInt.asInstanceOf[Integer]
            case _ => value
          }
        }
    }
    //    params foreach (v=> if(v==null) print("null") else print(v.getClass() + ":"+v))
    created._1.newInstance(params: _*).asInstanceOf[T]
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
  def insertBatch[T <: BDBEntity](data:List[T])={
    val request = new BatchWriteRowRequest()
    data.foreach(d=>request.addRowChange(d.getInstallRowChange()))
    reTry(3) {
      val result = client.batchWriteRow(request)
      result.getRowStatus
    }
  }
}
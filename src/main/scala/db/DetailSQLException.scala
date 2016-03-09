package db

import java.sql.SQLException

/** 包含Sql语句的异常
  *
  * @author zixuan
  *         The Class DetailSQLException.
  */
class DetailSQLException(cause: Throwable, val sql: String) extends SQLException(cause) {

  override def getMessage() = {
    sql + "\r\n" + super.getMessage()
  }

  override def toString() = {
    sql + "\r\n" + super.toString()
  }

}

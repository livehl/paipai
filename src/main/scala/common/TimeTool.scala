package common

import java.util.Date
import java.text.SimpleDateFormat
import java.util.Calendar

object TimeTool {
  def getFormatStringByNow() = {
    getFormatStringByDate(new Date())
  }

  def getFormatStringByDate(date: Date): String = {
    if (null == date) {
      null
    } else {
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
    }
  }

  def parseStringToDate(dateString: String) = {
    new SimpleDateFormat("yyyy-MM-dd").parse(dateString)
  }

  def getDateByNow()={
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY,0)
    cal.set(Calendar.MINUTE,0)
    cal.set(Calendar.SECOND,0)
    cal.set(Calendar.MILLISECOND,0)
    cal.getTime
  }
  def parseStringToDateTime(dateString: String) = {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateString)
  }

  def getDateStringByDate(date: Date): String = {
    if (null == date) {
      null
    } else {
      new SimpleDateFormat("yyyy-MM-dd").format(date)
    }
  }
  def getStringByFormat(date: Date = null, exp: String = "yyyy-MM-dd HH:mm:ss")={
    if (null == date) {
      null
    } else {
      new SimpleDateFormat(exp).format(date)
    }
  }

  def getDayString(date: Date = null, exp: String = null) = {
    val firstDate =
      if (date == null) {
        new Date()
      } else {
        date
      }
    val secDate = if (null != exp) {
      getAfterDate(exp, firstDate)
    } else {
      firstDate
    }
    new SimpleDateFormat("yyyy-MM-dd").format(secDate)
  }

  def getAfterDate(strExp: String, date: Date = new Date()) = {
    val exp = strExp.substring(strExp.length() - 1)
    val add = Integer.valueOf(strExp.substring(0, strExp.length() - 1))
    val cal = Calendar.getInstance()
    cal.setTime(date)
    exp.trim match {
      case "s" => cal.add(Calendar.SECOND, add)
      case "m" => cal.add(Calendar.MINUTE, add)
      case "H" => cal.add(Calendar.HOUR, add)
      case "d" => cal.add(Calendar.DAY_OF_MONTH, add)
      case "M" => cal.add(Calendar.MONTH, add)
      case "y" => cal.add(Calendar.YEAR, add)
    }
    cal.getTime()
  }

  /*
  获取日期差距
  date1 > date2 return >0
   */
  def getDayDiff(date1: Date, date2: Date = new Date()): Int = {
    val cal1 = Calendar.getInstance()
    cal1.setTime(date1)
    cal1.set(Calendar.HOUR, 0)
    cal1.set(Calendar.MINUTE, 0)
    cal1.set(Calendar.SECOND, 0)
    val cal2 = Calendar.getInstance()
    cal2.setTime(date2)
    cal2.set(Calendar.HOUR_OF_DAY, 0)
    cal2.set(Calendar.MINUTE, 0)
    cal2.set(Calendar.SECOND, 0)
    val diff = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR)
    if (diff != 0) {
      //不是同一年，直接整除一天毫秒数
      val nd = 1000 * 24 * 60 * 60 // 一天的毫秒数
      val daydiff = (date1.getTime() / nd) - (date2.getTime() / nd)
      daydiff.toInt
    } else cal1.get(Calendar.DAY_OF_YEAR) - cal2.get(Calendar.DAY_OF_YEAR)
  }

  def calTimeUse(fun: Any => Any): Any = {
    val start = System.currentTimeMillis()
    val result = fun
    val end = System.currentTimeMillis()
    println("use time:" + (end - start))
    result
  }

  implicit class StringtoDateMethod[A <: String](bean: A) {
    /** 字符转为日期 */
    def toDate = if (null == bean || bean.isEmpty) null else parseStringToDate(bean)

    def toDateTime = if (null == bean || bean.isEmpty) null else parseStringToDateTime(bean)
  }

  implicit class DateAddMethod[A <: Date](bean: A) {
    def sdate = TimeTool.getFormatStringByDate(bean)

    def sdatetime = TimeTool.getDateStringByDate(bean)

    def toCal={
      val cal = Calendar.getInstance()
      cal.setTime(bean)
      cal
    }
    def toSqlDate=if (null == bean) null else new java.sql.Date(bean.getTime)
  }

  implicit class CalAddMethod[A <: Calendar](bean: A) {
    def sdate = TimeTool.getFormatStringByDate(bean.getTime)
    def sdatetime = TimeTool.getDateStringByDate(bean.getTime)
  }

  def getTimeValue={
    System.currentTimeMillis()
  }
  def printTimeValue(time:Long,mark:String="",fv:Int=100){
    val v=System.currentTimeMillis()-time
    if(v>fv) {
      if (mark.isEmpty)
        println(v)
      else println(s"$mark use time:" + v)
    }
  }

}
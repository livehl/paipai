package tools

import collection.JavaConversions._
import scala.Array.canBuildFrom

object ExceptionTool {

  /**
   * 裁取堆栈,仅筛选包含当前项目的包
   *
   * the tx
   * @return the throwable
   * @author 黄林
   */
  lazy val packageString = {
    ExceptionTool.getClass.getName.split('.').take(3).mkString(".")
  }

  def cutStackTrace(tx: Throwable): Throwable = {
    cutStackTrace(tx, packageString, false)
  }

  /**
   * 裁取堆栈,仅筛选包含指定包的条目.
   *
   * @param tx
	 * the tx
   * @param packName
	 * the pack name
   * @param printHead
	 * 是否打印第一次出现包以前的详细堆栈
   * @return the throwable
   * @author 黄林 Cut stack trace.
   */
  def cutStackTrace(tx: Throwable, packName: String,
                    printHead: Boolean) = {
    val stes = tx.getStackTrace().filter(_.toString().contains(packName))
    if (printHead) {
      def getFrist(ste: Array[StackTraceElement]): Array[StackTraceElement] = {
        val res = new Array[StackTraceElement](0)
        if (ste.size > 0 && !ste(0).toString().contains(packName)) {
          (res :+ ste(0)) ++ getFrist(ste.drop(1))
        } else {
          res
        }
      }
      val heads = getFrist(tx.getStackTrace())
      tx.setStackTrace(stes ++ heads);
    } else {
      tx.setStackTrace(stes);
    }
    tx
  }

  /**
   * 裁取堆栈,仅筛选包含cn.city.in的包,包含前段错误堆栈
   *
   * @param tx
	 * the tx
   * @return the throwable
   * @author 黄林
   */
  def cutStackTraceWithHead(tx: Throwable) = {
    cutStackTrace(tx, packageString, true)
  }

  /**
   * 获取所有堆栈的文本消息
   *
   * @return the all
   * @author 黄林
   */
  def getAllStackTraces() = {
    val threadDumpMap = Thread
      .getAllStackTraces();
    val sb = new StringBuffer();
    val threadDump = threadDumpMap
      .entrySet();
    for (entry <- threadDump) {
      sb.append(entry.getKey().getName() + "\r\n");
      for (ste <- entry.getValue()) {
        sb.append("\t" + ste.toString() + "\r\n");
      }
    }
    sb.toString()
  }

  /**
   * 迭代输出堆栈为字符串
   *
   * @param tx
	 * the tx
   * @return the stack trace string
   * @author 黄林
   */
  def getStackTraceString(tx: Throwable): String = {
    val sb = new StringBuffer(tx.getMessage() + "\r\n")
    for (st <- tx.getStackTrace()) {
      sb.append(st.toString() + "\r\n")
    }
    if (null != tx.getCause()) {
      sb.append("\t" + getStackTraceString(tx.getCause()) + "\r\n");
    }
    sb.toString()
  }
}

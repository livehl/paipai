/**
  * Created by isaac on 16/4/15.
  */
object Test {

  implicit class IntAddMethod[A <: Int](bean: A) {
    def moneyStr(): String = {
      if (bean > 10000) (bean / 10000 + "万") else (bean + "")
    }
  }


  def main(array: Array[String]): Unit = {
    //计算每个月投资固定金额,需要多久会变成一千万
    //年利率
    val rate = 0.22d
    //起始资金
    val startMoney = 0d
    var money = startMoney
    //目标资金一千万
    val outMoney = 10000000
    //每月投入资金
    var inMoney = 1000

    1 to 100000 foreach { i =>
      money = money * (1 + rate / 12) + inMoney
      println(money + ":" + i)
      if (money > outMoney) {
        println(s"起始投入${startMoney.toInt.moneyStr},每月固定投入${inMoney.moneyStr},达到${outMoney.moneyStr} 需要" + i / 12 + "年" + i % 12 + "月")
        System.exit(0)
      }
    }
  }
}

import common.TimeTool

/**
  * Created by admin on 4/1/2017.
  */
object TimeTest {
  def main(args: Array[String]): Unit = {
      while(true){
        val time=TimeTool.getTimeValue
        List(1,2,3,4,5,6).foreach{v=>
          TimeTool.printTimeValue(time,"use time",0)
        }
      }
  }

}

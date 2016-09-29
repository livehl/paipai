/**
  * Created by admin on 2016/9/29.
  */

import com.amd.aparapi.Kernel
import com.amd.aparapi.Range

object GPU {
  def main(args: Array[String]): Unit = {
    //初始化输入输出数据
    val size: Int = 3090000
    val arrayIn: Array[Float] = new Array[Float](size)
    0 until size foreach(i=> arrayIn(i)=i)
    val arrayOut: Array[Float] = new Array[Float](size)

    /**gpu线程
      * */
    val kernel: Kernel = new Kernel() {
      @Override def run() {
        val gid: Int = getGlobalId()
        val num=Math.sqrt(arrayIn(gid) * arrayIn(gid)).toFloat
        if(num>10000)
        arrayOut(gid) = num/10000
        else
          arrayOut(gid) = num
      }
    }

    val time=System.currentTimeMillis()
    kernel.setExecutionMode(Kernel.EXECUTION_MODE.GPU)
    //执行计算
    kernel.execute(Range.create(3090000))
    println("use time:"+(System.currentTimeMillis()-time))

    System.out.println("Execution mode=" + kernel.getExecutionMode)

    kernel.dispose
  }

}

package tools

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import net.jpountz.lz4.LZ4Factory

/**
 * Created by yf on 15/11/13.
 */
object Z4ZTool {
  val factory = LZ4Factory.fastestInstance()
  val faster= factory.fastCompressor()
  val fasterUn=factory.fastDecompressor()

  implicit class DataCompressAddMethod[A <: Array[Byte]](data: A) {
    def z4z = z4zcompress(data)
    def unz4z=z4zuncompress(data)
    def unz4zStr=new String(z4zuncompress(data),"utf-8")
  }

  implicit class StringCompressAddMethod(str: String) {
    def z4z = z4zcompress(str.getBytes("utf-8"))
  }

  def z4zcompress(data:Array[Byte])={
    Array.concat(intToBytes(data.length),faster.compress(data))
  }

  def z4zuncompress(data:Array[Byte])={
    val len=bytesToInt(data.take(4),0)
    fasterUn.decompress(data,4,len)
  }

  /**
   * byte数组中取int数值
   *
   * @param ary
     *            byte数组
   * @param offset
     *            从数组的第offset位开始
   * @return int数值
   */
  def bytesToInt(ary:Array[Byte],offset:Int=0)={
    val buf = new ByteArrayInputStream(ary)
    val in = new DataInputStream(buf)
    if(offset>0)in.skip(offset)
    val v=in.readInt()
    in.close()
    buf.close()
    v
  }

  /**
   * byte数组转Int
   * @param v
   * @return
   */
  def intToBytes(v:Int)={
    val buf=new ByteArrayOutputStream()
    val out=new DataOutputStream(buf)
    out.writeInt(v)
    out.flush()
    val ary=buf.toByteArray
    out.close()
    buf.close()
    ary
  }

}

package common

import java.io._

import com.aliyun.oss._
import com.aliyun.oss.model._
import com.typesafe.config.ConfigFactory

import scala.collection.convert.WrapAsScala._
import scala.collection.convert.WrapAsJava._

object Aliyun{
  private lazy val conf = ConfigFactory.load()
  private lazy val fs = new Aliyun( conf.getString("ali.accessId"), conf.getString("ali.accessKey"), conf.getString("oss.bucket"),conf.getString("oss.endpoint"))
  def saveFile(path:String,data:Array[Byte]):Boolean=fs.saveFile(path,data)
  def getFile(path: String)=fs.getFile(path)
}



class Aliyun(val aid: String, val akey: String, val bucketName: String, val endpoint: String) {

  val client = {
    val conf = new ClientConfiguration()
    conf.setMaxConnections(100)
    conf.setConnectionTimeout(5000)
    conf.setMaxErrorRetry(3)
    conf.setSocketTimeout(2000)
    new OSSClient(endpoint, aid, akey, conf)
  }

  def getFile(fileName: String): Option[Array[Byte]] = {
    try {
      Some(Stream2Byte(client.getObject(bucketName, fileName).getObjectContent))
    } catch {
      case _: Throwable => None
    }
  }

  def saveFile(fileName: String, data: Array[Byte], ct: String = "") = {
    val meta = new ObjectMetadata()
    // 必须设置ContentLength
    meta.setContentLength(data.length)
    if (null != ct && !ct.isEmpty) {
      meta.setContentType(ct)
      meta.setContentDisposition("")
    }
    // 上传Object.
    val result = client.putObject(bucketName, fileName, Byte2Stream(data), meta)
    true
  }

  def saveFile(fileName: String, data: Array[Byte]):Boolean = {
    saveFile(fileName, data, "")
  }

  def existFile(fileName: String): Boolean = {
    try {
      client.getObjectMetadata(bucketName, fileName).getETag
      true
    } catch {
      case _: Throwable => false
    }
  }

  def deleteFile(fileName: String): Boolean = {
    try {
      client.deleteObject(bucketName, fileName)
      true
    } catch {
      case _: Throwable => false
    }
  }

  def deleteDir(dir: String): Boolean = {
    try {
      val listObjectsRequest = new ListObjectsRequest(bucketName);

      // 递归列出fun目录下的所有文件
      listObjectsRequest.setPrefix("fun/");

      val listing = client.listObjects(listObjectsRequest);

      val dor = new DeleteObjectsRequest(bucketName)
      dor.setKeys(listing.getObjectSummaries.map(_.getKey()).toList)
      client.deleteObjects(dor)
      true
    } catch {
      case _: Throwable => false
    }
  }

  def copyFile(source: String, dist: String): Boolean = {
    try {
      client.copyObject(bucketName, source, bucketName, dist)
      true
    } catch {
      case _: Throwable => false
    }
  }

  def Byte2Stream(data: Array[Byte]) = {
    new ByteArrayInputStream(data)
  }

  def Stream2Byte(is: InputStream) = {
    val baos = new ByteArrayOutputStream
    var b = is.read()
    while (b != -1) {
      baos.write(b)
      b = is.read()
    }
    baos.toByteArray
  }

  def File2Byte(file: File): Array[Byte] = {
    Stream2Byte(new FileInputStream(file))
  }

}

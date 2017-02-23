package tools

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.config.{SocketConfig, ConnectionConfig, RegistryBuilder}
import org.apache.http.conn.socket.{LayeredConnectionSocketFactory, PlainConnectionSocketFactory, ConnectionSocketFactory}
import org.apache.http.entity.ContentType

import scala.io.Source
import scala.util.parsing.json.JSON
import java.io.File
import scala.collection.JavaConversions.seqAsJavaList
import org.apache.http.client.CookieStore
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{ HttpGet, HttpPost }
import org.apache.http.entity.mime.{MultipartEntityBuilder, MultipartEntity}
import org.apache.http.entity.mime.content.{ FileBody, StringBody }
import org.apache.http.impl.client._
import org.apache.http.impl.conn.{PoolingHttpClientConnectionManager, PoolingClientConnectionManager}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.{HttpConnectionParams, BasicHttpParams}
import org.apache.http.client.config
import org.apache.http.{HttpHost, config, HttpEntity}
import org.apache.http.util.EntityUtils
import common.Tool._
import java.security.cert.X509Certificate
import javax.net.ssl.{TrustManager, SSLContext, X509TrustManager}
import org.apache.http.conn.ssl.{AllowAllHostnameVerifier,  SSLConnectionSocketFactory, SSLSocketFactory}
import org.apache.http.conn.scheme.Scheme

/**
 * Created by 林 on 14-3-25.
 */
object NetTool {
  val params = {
    //设置请求和传输超时时间
    RequestConfig.custom()
      .setSocketTimeout(10000)//.setProxy(new HttpHost("127.0.0.1",8888))
      .setConnectTimeout(10000).build()
  }
  val connManager ={
    val cm=new PoolingHttpClientConnectionManager()
    cm.setMaxTotal(200)
    cm.setDefaultMaxPerRoute(20)
    cm
  }
  def requestServer(url: String, method: String = "get", cookie: CookieStore = null, data: Map[String, String] = null, appendHead: Map[String, String] = null, files: Map[String, File] = null, encoding: String = "utf-8", useConnmanager: Boolean = true,httpReTryCount:Int=5,entity:HttpEntity=null) = {
    val ahead = (if (appendHead != null) appendHead else Map[String, String]())
    val head=if(ahead.contains("User-Agent")) ahead else ahead + ("User-Agent" -> ("	Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0"))
    val requestMethod = if (method == "get") new HttpGet(url) else new HttpPost(url)
    head.foreach(entry => requestMethod.setHeader(entry._1, entry._2))
    if (files != null && data != null) {
      val entry =MultipartEntityBuilder.create()
      data foreach (p => entry.addPart(p._1, new StringBody(p._2, ContentType.DEFAULT_TEXT)))
      files foreach (p => entry.addPart(p._1, new FileBody(p._2)))
      requestMethod.asInstanceOf[HttpPost].setEntity(entry.build())
    } else if (data != null) {
      requestMethod.asInstanceOf[HttpPost].setEntity(new UrlEncodedFormEntity(createNameValueList(data), encoding))
    }
    if(entity!=null){
      requestMethod.asInstanceOf[HttpPost].setEntity(entity)
    }
    val client = wrapClient(getHttpClient(useConnmanager))
    val context = HttpClientContext.create()
    if (null != cookie) context.setCookieStore(cookie)
    var content: HttpEntity = null
    reTry(httpReTryCount) {
      val response = client.execute(requestMethod,context)
      val status = response.getStatusLine().getStatusCode()
      content =response.getEntity()
      if (status != 200 && status != 302) {
        throw new Exception(status + ":" + EntityUtils.toString(content,encoding))
      }
    }
    (context.getCookieStore(), content)
  }

  def createNameValueList(params: Map[String, String]) = {
    (for ((k, v) <- params)
    yield new BasicNameValuePair(k, v)).toList
  }

  /**
   * 执行http get方法，返回状态和内容
   */
  def HttpGet(url: String, cookie: CookieStore = null, appendHead: Map[String, String] = null, encoding: String = "utf-8") = {
    val v=requestServer(url, "get", cookie, appendHead = appendHead, encoding = encoding)
    (v._1,EntityUtils.toString(v._2,encoding))
  }
  def HttpGetBin(url: String, cookie: CookieStore = null, appendHead: Map[String, String] = null, encoding: String = "utf-8") = {
    val v=requestServer(url, "get", cookie, appendHead = appendHead, encoding = encoding)
    (v._1,EntityUtils.toByteArray(v._2))
  }
  /**
   * 执行http post方法，返回状态和内容
   */
  def HttpPost(url: String, cookie: CookieStore = null, data: Map[String, String] = null, appendHead: Map[String, String] = null, files: Map[String, File] = null, encoding: String = "utf-8",entity:HttpEntity=null) = {
    val v=requestServer(url, "post", cookie, data, appendHead, files, encoding,entity=entity)
    (v._1,EntityUtils.toString(v._2,encoding))
  }
  def HttpPostBin(url: String, cookie: CookieStore = null, data: Map[String, String] = null, appendHead: Map[String, String] = null, files: Map[String, File] = null, encoding: String = "utf-8",entity:HttpEntity=null) = {
    val v=requestServer(url, "post", cookie, data, appendHead, files, encoding,entity=entity)
    (v._1,EntityUtils.toByteArray(v._2))
  }
   def getHttpClient(useConnmanager: Boolean = true) = {
    val client=HttpClientBuilder.create().setDefaultRequestConfig(params)
    if (useConnmanager) client.setConnectionManager(connManager)
    client.build()
  }


  /**
   * 在线查询ip数据
 *
   * @param ip
   * @return None or IP data
   */
  def queryIP(ip: String) = {
    try{
    val result = JSON.parseFull(Source.fromURL("http://ip.taobao.com/service/getIpInfo.php?ip=" + ip).getLines().mkString)
    result.getOrElse(null) match {
      case m: Map[_, _] =>
        m.asInstanceOf[Map[String,Any]].get("code").getOrElse(null) match {
          case 0 => Some(m.asInstanceOf[Map[String,Any]].get("data"))
          case 1 => None
          case _ => None
        }
      case _ => None
    }
  }catch {
      case e:Exception => None
    }

  }
  def wrapClient(base:CloseableHttpClient):CloseableHttpClient=
  {
    try {
      val registryBuilder = RegistryBuilder.create[ConnectionSocketFactory]()
      val plainSF = new PlainConnectionSocketFactory()
      registryBuilder.register("http", plainSF)
      val ctx = SSLContext.getInstance("TLS")
      val tm = new X509TrustManager() {
        def checkClientTrusted(chain:Array[X509Certificate],
          authType:String)={new Array[X509Certificate](0)}
        def checkServerTrusted(chain:Array[X509Certificate],
          authType:String)={}
        def getAcceptedIssuers():Array[X509Certificate]={null}
      }
      ctx.init(null, Array[TrustManager](tm), null)
      val sslSF = new SSLConnectionSocketFactory(ctx,new AllowAllHostnameVerifier)
      registryBuilder.register("https",sslSF)
      return HttpClientBuilder.create().setConnectionManager(connManager).setDefaultRequestConfig(params) .build()
    } catch{
      case ex:Exception=>ex.printStackTrace()
      return null
    }
  }

}

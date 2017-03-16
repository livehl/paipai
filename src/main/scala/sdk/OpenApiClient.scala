package sdk

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ppdai.open.core._
import java.io._
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

object OpenApiClient {
  /**
    * 获取授权信息URL
    */
  private val AUTHORIZE_URL: String = "https://ac.ppdai.com/oauth2/authorize"
  /**
    * 刷新Token信息URL
    */
  private val REFRESHTOKEN_URL: String = "https://ac.ppdai.com/oauth2/refreshtoken "
  private val dateformat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private var appid: String = null
  private var rsaCryptoHelper: RsaCryptoHelper = null

  @throws[Exception]
  def Init(appid: String, pkcsTyps: RsaCryptoHelper.PKCSType, publicKey: String, privateKey: String) {
    OpenApiClient.appid = appid
    rsaCryptoHelper = new RsaCryptoHelper(pkcsTyps, publicKey, privateKey)
  }

  /**
    * 向拍拍贷网关发送请求
    *
    * @param url
    * @param propertyObjects
    * @return
    */
  @throws[Exception]
  def send(url: String, propertyObjects: PropertyObject*): Result = {
    return send(url, 1, null, propertyObjects:_*)
  }

  /**
    * 向拍拍贷网关发送请求
    *
    * @param url
    * @param version
    * @param propertyObjects
    * @return
    */
  @throws[Exception]
  def send(url: String, version: Double, propertyObjects: PropertyObject*): Result = {
    return send(url, version, null, propertyObjects:_*)
  }

  /**
    * 向拍拍贷网关发送请求
    *
    * @param url
    * @param accessToken
    * @param propertyObjects
    * @return
    */
  @throws[Exception]
  def send(url: String, accessToken: String, propertyObjects: PropertyObject*): Result = {
    return send(url, 1, accessToken, propertyObjects:_*)
  }

  /**
    * 向拍拍贷网关发送请求
    *
    * @param url
    * @param accessToken
    * @param propertyObjects
    * @return
    */
  @throws[Exception]
  def send(url: String, version: Double, accessToken: String, propertyObjects: PropertyObject*): Result = {
    if (appid == null || "" == appid) throw new Exception("OpenApiClient未初始化")
    val result: Result = new Result
    try {
      val serviceUrl: URL = new URL(url)
      val urlConnection: HttpURLConnection = serviceUrl.openConnection.asInstanceOf[HttpURLConnection]
      urlConnection.setDoInput(true)
      urlConnection.setDoOutput(true)
      urlConnection.setUseCaches(false)
      /** ************ OpenApi所有的接口都只提供Post方法 **************/
      urlConnection.setRequestMethod("POST")
      urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
      urlConnection.setRequestProperty("X-PPD-SIGNVERSION", "1")
      urlConnection.setRequestProperty("X-PPD-SERVICEVERSION", String.valueOf(version))
      /** ***************** 公共请求参数 ************************/
      urlConnection.setRequestProperty("X-PPD-APPID", appid)
      //获取UTC时间作为时间戳
      val cal: Calendar = java.util.Calendar.getInstance
      val zoneOffset: Int = cal.get(java.util.Calendar.ZONE_OFFSET)
      val dstOffset: Int = cal.get(java.util.Calendar.DST_OFFSET)
      cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset))
      val timestamp: Long = (cal.getTime.getTime - dateformat.parse("1970-01-01 00:00:00").getTime) / 1000
      urlConnection.setRequestProperty("X-PPD-TIMESTAMP", timestamp.toString)
      //对时间戳进行签名
      urlConnection.setRequestProperty("X-PPD-TIMESTAMP-SIGN", rsaCryptoHelper.sign(appid + timestamp).replaceAll("\\r", "").replaceAll("\\n", ""))
      val sign: String = rsaCryptoHelper.sign(ObjectDigitalSignHelper.getObjectHashString(propertyObjects:_*)).replaceAll("\\r", "").replaceAll("\\n", "")
      urlConnection.setRequestProperty("X-PPD-SIGN", sign)
      if (accessToken != null &&  "" != accessToken) urlConnection.setRequestProperty("X-PPD-ACCESSTOKEN", accessToken)
      /** ************************************************************/
      val dataOutputStream: DataOutputStream = new DataOutputStream(urlConnection.getOutputStream)
      dataOutputStream.writeBytes(propertyToJson(propertyObjects:_*))
      dataOutputStream.flush()
      val inputStream: InputStream = urlConnection.getInputStream
      val inputStreamReader: InputStreamReader = new InputStreamReader(inputStream, "utf-8")
      val bufferedReader: BufferedReader = new BufferedReader(inputStreamReader)
      val strResponse: String = bufferedReader.readLine
      result.setSucess(true)
      result.setContext(strResponse)
    }
    catch {
      case e: UnsupportedEncodingException => {
        e.printStackTrace()
        result.setErrorMessage(e.getMessage)
      }
      case e: ProtocolException => {
        e.printStackTrace()
        result.setErrorMessage(e.getMessage)
      }
      case e: MalformedURLException => {
        e.printStackTrace()
        result.setErrorMessage(e.getMessage)
      }
      case e: IOException => {
        e.printStackTrace()
        result.setErrorMessage(e.getMessage)
      }
      case e: Exception => {
        e.printStackTrace()
        result.setErrorMessage(e.getMessage)
      }
    } finally {
    }
    return result
  }

  /**
    * @param propertyObjects
    * @return
    */
  @throws[JsonProcessingException]
  private def propertyToJson(propertyObjects: PropertyObject*): String = {
    val mapper: ObjectMapper = new ObjectMapper
    val node: ObjectNode = mapper.createObjectNode
    for (propertyObject <- propertyObjects) {
      if (propertyObject.getValue.isInstanceOf[Integer]) {
        node.put(propertyObject.getName, propertyObject.getValue.asInstanceOf[Integer])
      }
      else if (propertyObject.getValue.isInstanceOf[Long]) {
        node.put(propertyObject.getName, propertyObject.getValue.asInstanceOf[Long])
      }
      else if (propertyObject.getValue.isInstanceOf[Float]) {
        node.put(propertyObject.getName, propertyObject.getValue.asInstanceOf[Float])
      }
      else if (propertyObject.getValue.isInstanceOf[BigDecimal]) {
        node.put(propertyObject.getName, propertyObject.getValue.asInstanceOf[BigDecimal])
      }
      else if (propertyObject.getValue.isInstanceOf[Double]) {
        node.put(propertyObject.getName, propertyObject.getValue.asInstanceOf[Double])
      }
      else if (propertyObject.getValue.isInstanceOf[Boolean]) {
        node.put(propertyObject.getName, propertyObject.getValue.asInstanceOf[Boolean])
      }
      else if (propertyObject.getValue.isInstanceOf[String]) {
        node.put(propertyObject.getName, propertyObject.getValue.asInstanceOf[String])
      }
      else if (propertyObject.getValue.isInstanceOf[Date]) {
        node.put(propertyObject.getName, dateformat.format(propertyObject.getValue.asInstanceOf[Date]))
      }
      else {
        node.put(propertyObject.getName, mapper.convertValue(propertyObject.getValue,classOf[JsonNode]))
      }
    }
    return mapper.writeValueAsString(node)
  }

  /**
    * 获取授权
    *
    * @param code 授权码
    * @return
    * @throws IOException
    */
  @throws[Exception]
  def authorize(code: String): AuthInfo = {
    if (appid == null || "" == appid) throw new Exception("OpenApiClient未初始化")
    val serviceUrl: URL = new URL(AUTHORIZE_URL)
    val urlConnection: HttpURLConnection = serviceUrl.openConnection.asInstanceOf[HttpURLConnection]
    urlConnection.setDoInput(true)
    urlConnection.setDoOutput(true)
    urlConnection.setUseCaches(false)
    urlConnection.setRequestMethod("POST")
    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
    val dataOutputStream: DataOutputStream = new DataOutputStream(urlConnection.getOutputStream)
    /** ****************** 获取授权参数 AppID code *********************/
    dataOutputStream.writeBytes(String.format("{\"AppID\":\"%s\",\"code\":\"%s\"}", appid, code))
    dataOutputStream.flush()
    val inputStream: InputStream = urlConnection.getInputStream
    val inputStreamReader: InputStreamReader = new InputStreamReader(inputStream, "utf-8")
    val bufferedReader: BufferedReader = new BufferedReader(inputStreamReader)
    val strResponse: String = bufferedReader.readLine
    val mapper: ObjectMapper = new ObjectMapper
    return mapper.readValue(strResponse, classOf[AuthInfo])
  }

  /**
    * 刷新AccessToken
    *
    * @param openId       用户OpenID
    * @param refreshToken 刷新Token
    * @return
    * @throws IOException
    */
  @throws[Exception]
  def refreshToken(openId: String, refreshToken: String): AuthInfo = {
    if (appid == null || "" == appid) throw new Exception("OpenApiClient未初始化")
    val serviceUrl: URL = new URL(REFRESHTOKEN_URL)
    val urlConnection: HttpURLConnection = serviceUrl.openConnection.asInstanceOf[HttpURLConnection]
    urlConnection.setDoInput(true)
    urlConnection.setDoOutput(true)
    urlConnection.setUseCaches(false)
    urlConnection.setRequestMethod("POST")
    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
    val dataOutputStream: DataOutputStream = new DataOutputStream(urlConnection.getOutputStream)
    /** **************** 刷新Token参数 AppID OpenID RefreshToken **********************/
    dataOutputStream.writeBytes(String.format("{\"AppID\":\"%s\",\"OpenID\":\"%s\",\"RefreshToken\":\"%s\"}", appid, openId, refreshToken))
    dataOutputStream.flush()
    val inputStream: InputStream = urlConnection.getInputStream
    val inputStreamReader: InputStreamReader = new InputStreamReader(inputStream, "utf-8")
    val bufferedReader: BufferedReader = new BufferedReader(inputStreamReader)
    val strResponse: String = bufferedReader.readLine
    val mapper: ObjectMapper = new ObjectMapper
    return mapper.readValue(strResponse, classOf[AuthInfo])
  }
}
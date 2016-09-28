package tools

import common.Tool._
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder

/**
  * Created by admin on 2016/9/28.
  */
object Image {
  def getImageCode(data:Array[Byte])={
    val entity=MultipartEntityBuilder.create()
    entity.addTextBody("username","livehl")
    entity.addTextBody("password","19890218")
    entity.addTextBody("typeid","1040")
    entity.addTextBody("softid","67954")
    entity.addTextBody("softkey","33a6fb701d3a4607ae930ff4bb67c43d")
    entity.addBinaryBody("image",data, ContentType.DEFAULT_BINARY,"image.jpg")
    val (_,result)=NetTool.HttpPost("http://api.ruokuai.com/create.json",entity=entity.build())
    result.jsonToMap("Result").toString
  }

}

package common

/**
  * Created by isaac on 16/2/2.
  */
object StringTool {

  def getNoExp(content:String,split:String="{",endSplit:String="}")={
    if(null!=content && content.length>0){
      if(content.contains(split)&&content.contains(endSplit)){
        content.replace("("+getExpBody(content,split,endSplit)+")","")
      } else  content
    } else content
  }

  def getExpBody(content:String,split:String="{",endSplit:String="}")={
    if(null!=content && content.length>0){
      if(content.contains(split)&&content.contains(endSplit)){
        val noLeftValue=content.drop(content.indexOf(split)+1)
        noLeftValue.dropRight(noLeftValue.length - noLeftValue.lastIndexOf(endSplit))
      }else ""
    } else ""
  }

}

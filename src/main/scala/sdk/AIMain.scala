package sdk

import java.io.{File, FileOutputStream}

import common.Tool._
import db._
import tools.NetTool

/**
  * Created by admin on 2016/9/9.
  */
object AIMain {
  def main(args: Array[String]) {
    //    fullData
    getVerifyData
    //    saveFile("ppdverify.csv",getVerifyData)
  }

  //填充遗失的数据
  def fullData = {
    val okIds = new RetList().queryAll().map(_.ListingId.toInt).toSet[Int]
    val failIds = new BackList().queryAll().map(_.ListingId.toInt).toSet[Int]
    val iiIds = (okIds ++ failIds).toList.grouped(200).map { ids =>
      new LoanInfo().query(s"ListingId in ( ${(okIds ++ failIds) map (v => "'" + v + "'") mkString (",")})")
    }.flatten.map(v => v.ListingId).toSet[Int]
    val noDataIds = (okIds ++ failIds).filterNot(v => iiIds.contains(v))
    val loanInfos = LoansApi.getLoanInfo(noDataIds.toList)
    println("add info size:" + loanInfos.size)
    loanInfos.mutile(10).foreach(_.insert())
    val loanIds = (okIds ++ failIds).toList.grouped(200).map { ids =>
      new Loan().query(s"ListingId in ( ${ids map (v => "'" + v + "'") mkString (",")})")
    }.flatten.map(v => v.ListingId).toSet[Int]
    val noIds = (okIds ++ failIds).filterNot(v => loanIds.contains(v))
    val loans = LoansApi.getLoanWithId(noIds.toList)
    println("add loan size:" + loans.size)
    //    println(loans.toJson())
    loans.mutile(10).foreach(_.insert())
  }

  def getTrainData = {
    val okIds = new RetList().queryAll().map(_.ListingId).toSet[String]
    val failIds = new BackList().queryAll().map(_.ListingId).toSet[String]
    val loans = (okIds ++ failIds).toList.grouped(200).map { ids =>
      new Loan().query(s"ListingId in ( ${ids map (v => "'" + v + "'") mkString (",")})")
    }.flatten.map(v => v.ListingId.toString -> v).toMap
    val loanInfos = (okIds ++ failIds).toList.grouped(200).map { ids =>
      new LoanInfo().query(s"ListingId in ( ${(okIds ++ failIds) map (v => "'" + v + "'") mkString (",")})")
    }.flatten.map(v => v.ListingId.toString -> v).toMap
    println(s"ok size:${okIds.size} ,fail size: ${failIds.size},loan size:${loans.size},loanInfo size:${loanInfos.size}")
    val trainDatas = loanInfos.map(_._1).filter(v => loans.contains(v)).map { id =>
      val loan = loans(id)
      val loanInfo = loanInfos(id)
      val CreditCode = loan.CreditCode match {
        case "A" => 1
        case "B" => 2
        case "C" => 3
        case "D" => 4
        case "E" => 5
        case "F" => 6
        case _ => 0
      }
      val result = if (okIds.contains(id)) 1 else if (failIds.contains(id)) 0 else throw new Exception("不正确的数据")
      //    //装载ai数据
      val data = List(loanInfo.Gender, loanInfo.Age, loanInfo.SuccessCount, loanInfo.NormalCount, loanInfo.OverdueLessCount,
        loanInfo.OverdueMoreCount, loanInfo.OwingPrincipal, loanInfo.OwingAmount, loanInfo.CertificateValidate, loanInfo.NciicIdentityCheck,
        loanInfo.PhoneValidate, loanInfo.VideoValidate, loanInfo.CreditValidate, loanInfo.EducateValidate, loan.Amount, CreditCode,
        loan.Months, loan.Rate, result).mkString(",")
      data
    }.mkString("\r\n")
    //    println(trainDatas)
    trainDatas
  }

  def getVerifyData = {
    val verifyRow=0.97
    var failCount = 0
    var lossCount = 0
    val okIds = new RetList().queryAll().map(_.ListingId).toSet[String]
    val failIds = new BackList().queryAll().map(_.ListingId).toSet[String]
    val loans = (okIds ++ failIds).toList.grouped(200).map { ids =>
      new Loan().query(s"ListingId in ( ${ids map (v => "'" + v + "'") mkString (",")})")
    }.flatten.map(v => v.ListingId.toString -> v).toMap
    val loanInfos = (okIds ++ failIds).toList.grouped(200).map { ids =>
      new LoanInfo().query(s"ListingId in ( ${(okIds ++ failIds) map (v => "'" + v + "'") mkString (",")})")
    }.flatten.map(v => v.ListingId.toString -> v).toMap
    println(s"ok size:${okIds.size} ,fail size: ${failIds.size},loan size:${loans.size},loanInfo size:${loanInfos.size}")
    val verifyDatas = loanInfos.map(_._1).filter(v => loans.contains(v)).mutile(10).map { id =>
      val loan = loans(id)
      val loanInfo = loanInfos(id)
      val CreditCode = loan.CreditCode match {
        case "A" => 1
        case "B" => 2
        case "C" => 3
        case "D" => 4
        case "E" => 5
        case "F" => 6
        case _ => 0
      }
      val result = if (okIds.contains(id)) 1 else if (failIds.contains(id)) 0 else throw new Exception("不正确的数据")
      //    //装载ai数据
      val data = List(loanInfo.Gender, loanInfo.Age, loanInfo.SuccessCount, loanInfo.NormalCount, loanInfo.OverdueLessCount,
        loanInfo.OverdueMoreCount, loanInfo.OwingPrincipal, loanInfo.OwingAmount, loanInfo.CertificateValidate, loanInfo.NciicIdentityCheck,
        loanInfo.PhoneValidate, loanInfo.VideoValidate, loanInfo.CreditValidate, loanInfo.EducateValidate, loan.Amount, CreditCode,
        loan.Months, loan.Rate).mkString(",")
      val point = NetTool.HttpPost("http://127.0.0.1/predict", null, Map("data" -> data))._2.toBigDecimal
      //      println(result+",ai:" + loan.ListingId + "=" + point)
      if (point > verifyRow && result == 0) {
        failCount += 1
      }
      if (point <= verifyRow && result == 1) {
        lossCount += 1
      }
      data + "," + point.toString() + "," + result
    }.mkString("\r\n")
    //    println(trainDatas)
    println(s"allCount:${loanInfos.size},failCount:${failCount},fail:${BigDecimal(failCount * 100) / loanInfos.size},lossCount:${lossCount},loss:${BigDecimal(lossCount * 100) / loanInfos.size}")
    verifyDatas
  }

  def saveFile(file: String, content: String) = {
    val fos = new FileOutputStream(new File(file))
    fos.write(content.getBytes("utf-8"))
    fos.close()
    true
  }


}

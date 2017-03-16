package sdk

import java.util
import java.util.Date

import akka.actor.ActorRef
import com.ppdai.open.core.{PropertyObject, RsaCryptoHelper, ValueTypeEnum}
import common.Tool._
import db._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * Created by isaac on 16/3/9.
  */
object Loans {
  val loanLock:String=""

  val pubKey="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC6WOoXN/3wiNcQ5gcf0nCBpKSiaxw2dv8gSqkiwkjRQzaIl/iREVOPf2EbQf3HodvdDGx8S97SOWux2pRG2gdWfX5k+pXl7+8asrGRDK10HYXLXh1ROyApekFZXMwIh8MZIQui3vmIEhNXeD0egzbyL9zXm86RvYyIRjMhglPsQQIDAQAB"
  val privKey="MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAMZfZeS3tFRpTsutwSBBeqddl6gk5HXHGlaYu5WeyBQ+YPfOgI4TBQ89t58yRknEp4R7sPvEA3lfBmCe/tyrTM4EBvzLv2/DPjWk0jDFZawE4aUHK2KG50FOhp6YH8xyOwQrB/H/6YjW4J7q2OSoaOvWvE1G5gi85vK/we/zn4PFAgMBAAECgYBAp3ceRIGRwYDdAZSgXrcLNYXoV53ehTYgY0dATLAJaQtRuQxNQgW0Iflm+YvPHzk6BNZ6ODippj793tRSN8KgD+RbI5oZyM8IAmQk9gDgy5qAh35CutW7+KoJ7nhTXCc7ti1EoWMclECgffDyDWyxqFsaORmLmjW9lE2UG8jwAQJBAOVtZl9+0B+GAKYnFvhw87RQvhf/Mie10dFJwff1P98nzFFL5UC5V0Nq+9+ioCvjPf4DKKj9vv7hsa/AaY9NPIUCQQDdWTc24VmcteVlsJLfGmpPeMlhF71l/rqtrlpRhHZ36O/HmfqLtCYshImOjPF5RvwGNZCHCvC4cYFfpP7lmm5BAkA9n5PmxIYcYX7dIhS+aIBdB273vRj4p5KS12/dLSeZxfPQRkVujBnPRvYeTG0fPKtTBgAu2/EoPvDeFx2DWyiNAkAWaouR7j5yBWXG55voJjev9q6GO649nw9uuWKCMOUCfb+SukBKV6MqDP4VRqbJvmuVgWUyl+QK+cu9UOtTe1FBAkB2WGNRGEPn9NfYQntbYqEziyJecJeFFPahYrDOYq+Bv4S7YGUFHYypA1HtLhjqmRBOoGKtlDn4kUKDvaLtReTU"
  val gwurl="http://gw.open.ppdai.com"

  OpenApiClient.Init("300127ae80f843119d04d05b6db66de3", RsaCryptoHelper.PKCSType.PKCS8, pubKey, privKey)

  def main(args: Array[String]) {
    while(true) {
      val loans = getLoan(1).filter(_.Rate>=20)
      if(loans.size>0) {
        println(loans.map(_.ListingId).mkString(","))
        println(getLoanInfo(loans.map(_.ListingId)))
      }
      Thread.sleep(1000)
    }
  }
  def loanActor(loanActor: ActorRef){
    var oldlist=new ListBuffer[Int]()
    var i=1
    while (true) {
      safe {
        val lists = getLoan(i)
        if (System.currentTimeMillis() / 1000 % 30 == 1) {
          println(new Date().sdatetime + "page:" + i + ",size:" + lists.size)
        }
        //流
        val streamList=lists.filter(v => v.Rate >= 18).filter(v=> !oldlist.contains(v.ListingId))
        if(streamList.size>0){
          loanActor ! streamList
        }
        lists.filter(v=> !oldlist.contains(v.ListingId)).foreach{v=> oldlist.append(v.ListingId)}
        if(oldlist.size>=100){
          oldlist.remove(0,30)
        }
        if (lists.size < 2000) {
          i = 1
        } else {
          i += 1
        }
        Thread.sleep(1000)
      }
    }
  }

  def getLoan(page:Int)={
    val result = OpenApiClient.send(gwurl + "/invest/LLoanInfoService/LoanList", new PropertyObject("PageIndex", page, ValueTypeEnum.String))
    if (result.isSucess){
      result.getContext.jsonToMap("LoanInfos").asInstanceOf[List[Map[String,Any]]].map{l=>
        val m=l.map(kv=> kv._1->kv._2.toString)
        val funding= ((1 - m("RemainFunding").toDouble/(m("Amount").toDouble)) * 100).toInt
        new Loan(0,m("Title"),m("ListingId").toInt,m("Amount").toDouble,m("CreditCode"),funding,0,m("Months").toInt,m("Rate").toDouble,new Date(),null)
      }
    }else Nil
  }

  def getLoanInfo(loans: List[Int])={
    if(loans.size>0) {
      loans.grouped(10).toList.map{ids=>
        val result = OpenApiClient.send(gwurl + "/invest/LLoanInfoService/BatchListingInfos", new PropertyObject("ListingIds",ids.toList.asJava, ValueTypeEnum.Other))
        if (result.isSucess) {
                result.getContext.jsonToMap("LoanInfos").asInstanceOf[List[Map[String,Any]]].map{l=>
                  val jsonMap=l.filter(v=>loanInfoKeys.contains(v._1)) +("createTime"->new Date())
                  toBean(jsonMap.toJson,classOf[LoanInfo])                }
        } else Nil
      }.flatten
    }else Nil
  }

  val loanInfoKeys=Set("ListingId","GraduateSchool","NciicIdentityCheck","StudyStyle","BorrowName","CertificateValidate","OverdueMoreCount","PhoneValidate","OverdueLessCount","OwingAmount","Age","SuccessCount","CreditValidate","NormalCount","EducationDegree","Gender","VideoValidate","OwingPrincipal","EducateValidate")
}

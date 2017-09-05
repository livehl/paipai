import com.ppdai.open.core.{PropertyObject, ValueTypeEnum}
import sdk.{LoansApi, OpenApiClient, UserApi}
import common.Tool._
import sdk.LoansApi.gwurl

import scala.collection.JavaConverters._
/**
  * Created by admin on 2/22/2017.
  */
object OpenApi {

  def main(array: Array[String]): Unit = {
//    println(LoansApi.getLoanInfo(List(35374924)).toJson)
//    val result = OpenApiClient.send(gwurl + "/invest/LLoanInfoService/BatchListingInfos", new PropertyObject("ListingIds",List(35374924).toList.asJava, ValueTypeEnum.Other))
//    println(result.getContext)
//    UserApi.getAllRetList
//    UserApi.getAllBackList
    LoansApi.checkBidLoans
    System.exit(0)
  }

}

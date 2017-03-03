package main

import com.ppdai.open.core.{OpenApiClient, PropertyObject, RsaCryptoHelper, ValueTypeEnum}

/**
  * Created by admin on 3/3/2017.
  */
object PaiPaiSDK {
  val pubKey="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC6WOoXN/3wiNcQ5gcf0nCBpKSiaxw2dv8gSqkiwkjRQzaIl/iREVOPf2EbQf3HodvdDGx8S97SOWux2pRG2gdWfX5k+pXl7+8asrGRDK10HYXLXh1ROyApekFZXMwIh8MZIQui3vmIEhNXeD0egzbyL9zXm86RvYyIRjMhglPsQQIDAQAB"
  val privKey="MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAMZfZeS3tFRpTsutwSBBeqddl6gk5HXHGlaYu5WeyBQ+YPfOgI4TBQ89t58yRknEp4R7sPvEA3lfBmCe/tyrTM4EBvzLv2/DPjWk0jDFZawE4aUHK2KG50FOhp6YH8xyOwQrB/H/6YjW4J7q2OSoaOvWvE1G5gi85vK/we/zn4PFAgMBAAECgYBAp3ceRIGRwYDdAZSgXrcLNYXoV53ehTYgY0dATLAJaQtRuQxNQgW0Iflm+YvPHzk6BNZ6ODippj793tRSN8KgD+RbI5oZyM8IAmQk9gDgy5qAh35CutW7+KoJ7nhTXCc7ti1EoWMclECgffDyDWyxqFsaORmLmjW9lE2UG8jwAQJBAOVtZl9+0B+GAKYnFvhw87RQvhf/Mie10dFJwff1P98nzFFL5UC5V0Nq+9+ioCvjPf4DKKj9vv7hsa/AaY9NPIUCQQDdWTc24VmcteVlsJLfGmpPeMlhF71l/rqtrlpRhHZ36O/HmfqLtCYshImOjPF5RvwGNZCHCvC4cYFfpP7lmm5BAkA9n5PmxIYcYX7dIhS+aIBdB273vRj4p5KS12/dLSeZxfPQRkVujBnPRvYeTG0fPKtTBgAu2/EoPvDeFx2DWyiNAkAWaouR7j5yBWXG55voJjev9q6GO649nw9uuWKCMOUCfb+SukBKV6MqDP4VRqbJvmuVgWUyl+QK+cu9UOtTe1FBAkB2WGNRGEPn9NfYQntbYqEziyJecJeFFPahYrDOYq+Bv4S7YGUFHYypA1HtLhjqmRBOoGKtlDn4kUKDvaLtReTU"
  val gwurl="http://gw.open.ppdai.com"

  def main(args: Array[String]) {
    """手机实名认证不限。
      |普通专科及以上。
      |年龄22到56
      |魔镜等级：A,B,C,D。
      |性别不限。
      |列表期限1到12个月。
      |借款金额在100到20000之间
      |借款利率在18到24之间
      |成功还清借款安全期数以上"""
    OpenApiClient.Init("300127ae80f843119d04d05b6db66de3", RsaCryptoHelper.PKCSType.PKCS8, pubKey, privKey)
    val result = OpenApiClient.send(gwurl + "/invest/LLoanInfoService/LoanList", new PropertyObject("PageIndex", "1", ValueTypeEnum.String), new PropertyObject("DeviceFP", "asdfasdf4asdf546asf", ValueTypeEnum.Int32))
    if (result.isSucess){
      result.getContext
    }
  }

}

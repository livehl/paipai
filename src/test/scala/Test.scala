import java.io.{File, FileInputStream}
import java.util.Date

import common.{OtsCache, TimeTool}
import common.Tool._
import db._
import org.apache.http.entity.ByteArrayEntity
import sdk.UserApi
import tools.{Image, NetTool}

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Created by isaac on 16/4/15.
  */
object Test {



  def main(array: Array[String]): Unit = {
    UserApi.updateUsers
    System.exit(0)
//    runTasks

//    println(NetTool.HttpPost("http://ppdai.sftui.com/predict", null, Map("data" -> "2,25,2,5,2,0,5779,7676,0,0,1,0,0,0,5776,3,6,20"))._2)

//    println("1d".dateExp.sdatetime)
//    println(new TaskLog().createTable())
    val html="""
               | <div class="my-ac-users c666666 clearfix">
               |                <a href="http://www.ppdai.com/info/userface/" target="_blank">
               |                    <img class="fl"
               |                         src="http://static.ppdai.com/app_themes/images/head/nophoto_80.gif"
               |                         alt="用户头像" width="150px" height="150px"
               |                         style="margin-right:10px;" />
               |                </a>
               |
               |                <div class="w600 fl">
               |                    <div class="clearfix">
               |                        <div class="my-ac-paisafty fl">
               |                            <div class="my-ac-userverfy clearfix">
               |                                <label class="my-ac-username fl">您好，*林</label>
               |                                <a class="my-ac-viplever vip3 fl" title="等级:VIP3" href="http://invest.ppdai.com/vipInfo/index" target="_blank"></a>
               |                                    <span class="airBubble airBubble_high">
               |                                        <a href="http://help.ppdai.com/Home/List/32/246#246" class="my-ac-dgvip fl"  target="_blank"></a>
               |                                        <div class="tag">
               |                                        <div class="arrow"><em></em><span></span></div>
               |                                            您已成为高净值用户，当前净资产234242.37元。<a href="http://help.ppdai.com/Home/List/32" class="tip_link" target="_blank">查看更多</a>
               |                                        </div>
               |                                    </span>
               |                            </div>
               |                            <div class="my-ac-ps-one">
               |                                <div class="my-ac-ps-one">
               |                                    <label>拍<span class="ml28">币：</span></label>
               |                                    <span class="c39a1ea mr20" id="paiMoney">4106</span>
               |                                    <label class="my-ac-qiandao my-ac-qdq">
               |
               |                                        <span id="btnGetPaiMoney"  class=my-ac-qdsignb>签到</span><span style="margin: 0 4px; color: #d5d5d5;">|</span><a href="http://www.ppdai.com/account/paimoney/market" class="c39a1ea" target="_blank"><span>兑换</span></a>
               |                                    </label>
               |                                </div>
               |                            </div>
               |                            <div class="my-ac-ps-two clearfix">
               |                                <label class="fl">账户安全：</label>
               |                                    <a class="safelevel high fl" href="http://ac.ppdai.com/safe/setting" target="_blank">高</a>
               |
               |                                <a class="my-ac-userinfoicon icon_userinfo  fl" id="userinfo"
               |                                   href="http://invest.ppdai.com/UserInfoSupplement/Index" title="信息已完善，请定时更新"
               |                                   target="_blank"></a>
               |                                <a class="my-ac-userinfoicon icon_icard  fl"
               |                                   href="http://www.ppdai.com/cert/identitycert" title='已通过身份认证' target="_blank"></a>
               |                                <a class="my-ac-userinfoicon icon_phone  fl" title="已绑定手机号"
               |                                   href="http://ac.ppdai.com/userbind/bindmobile" target="_blank"></a>
               |                            </div>
               |                            <div class="lastLoginTime" style="color:#666666"></div>
               |                        </div>
               |                        <div class="pl35 pt14 fl" style="">
               |                            <div class="">
               |                                <p>可用余额(元)</p>
               |
               |                                <p class="my-ac-ps-yue"><label class="icon_money">&#165;</label>7,358.22</p>
               |                            </div>
               |                            <div>
               |                                <a class="my-orange-btn my-ac-btns" href="https://pay.ppdai.com/order/online" target="_blank">充值</a><a class="ml16 my-blue-btn my-ac-btns"
               |                                                                                                                                       href="https://pay.ppdai.com/trade/cashwithdrawal" target="_blank">提现</a>
               |                            </div>
               |
               |                            <div class="my-ac-ps-yhq">
               |                                <a class="my-ac-ps-yhqicon fl" href="http://www.ppdai.com/account/coupon" target="_blank"></a><a href="http://www.ppdai.com/account/coupon" target="_blank">0 张</a>&nbsp;&nbsp;<span>0.00元</span>
               |                            </div>
               |                        </div>
               |                    </div>
               |                </div>
               |            </div>
               |            <!-- 我的账户 end-->
               |            <!-- 资产情况 start-->
               |            <div class="my-ac-credittotalFr c666666">
               |                <div class="my-ac-ctTitle clearfix">
               |                    <label class="titletext fr">数据统计至：2017-09-18 0 点 </label>
               |                </div>
               |                <div class="my-ac-ctList clearfix">
               |                    <div class="my-ac-item fl">
               |                        <div class="my-ac-cl-one">
               |                            <span>昨日收益</span><span class="airBubble">
               |                                <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                <div class="tag">
               |                                    <div class="arrow"><em></em><span></span></div>
               |                                    各项投资昨日收益之和，已扣除坏账计提
               |                                </div>
               |                            </span>
               |                        </div>
               |                        <div class="my-ac-cl-two">
               |                                <span class='ft14'>¥</span>-79.10
               |                                <div class="data_show">
               |                                                                                                                <div><span class="show_title">散标</span>-79.10</div>
               |                                                                                                        </div>
               |                        </div>
               |                        <div class="my-ac-cl-three"></div>
               |                    </div>
               |                    <div class="my-ac-item fl">
               |                        <div class="my-ac-cl-one">
               |                            <span>累计收益</span><span class="airBubble">
               |                                <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                <div class="tag">
               |                                    <div class="arrow"><em></em><span></span></div>
               |                                    各项投资累计收益之和，已扣除坏账计提
               |                                </div>
               |                            </span>
               |                        </div>
               |                        <div class="my-ac-cl-two">
               |                                <span class='ft14'>¥</span>24,077.40
               |                                <div class="data_show">
               |
               |                                                                                                                                                    <div><span class="show_title">散标</span>23,832.40</div>
               |                                                                                                                                                    <div><span class="show_title">奖励
               |                                                 <span class="airBubble">
               |                                                     <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                                     <div class="tag">
               |                                                         <div class="arrow"><em></em><span></span></div>
               |                                                         成功使用彩虹或散标抵用券立减金额+拍活宝加息券产生的加息收益+万元体验金到期产生的收益
               |                                                     </div>
               |                                                 </span>
               |                                            </span>245.00</div>
               |                                </div>
               |                        </div>
               |                        <div class="my-ac-cl-three">
               |                        </div>
               |                    </div>
               |                    <div class="my-ac-item fl">
               |                        <div class="my-ac-cl-one">
               |                            待收收益<span class="airBubble">
               |                                <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                <div class="tag">
               |                                    <div class="arrow"><em></em><span></span></div>
               |                                    各项投资待收收益之和，已扣除坏账计提
               |                                </div>
               |                            </span>
               |                        </div>
               |                        <div class="my-ac-cl-two">
               |                                <span class='ft14'>¥</span>19,560.59
               |
               |                                <div class="data_show">
               |
               |                                                                                                                <div><span class="show_title">散标</span>19,560.59</div>
               |                                                                    </div>
               |                        </div>
               |                    </div>
               |                </div>
               |            </div>
               |            <!-- 资产情况 end-->
               |            <!-- 收益概况 start-->
               |            <div class="my-ac-credittotalFr c666666">
               |                <div class="my-ac-ctTitle clearfix">
               |                    <div class="fr">
               |                        <label class="titletext ">数据统计 : 实时</label>
               |                    </div>
               |                </div>
               |                <div class="my-ac-syList clearfix">
               |                    <div class="fl">
               |                        <div class="charts_all">
               |                            <div class="charts_title">
               |                                    总资产<span class="airBubble2">
               |                                    <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                    <div class="tag">
               |                                        <div class="arrow"><em></em><span></span></div>
               |                                        各项投资持有本金之和+投标中+取现中+可用余额（散标和债转标持有本金已扣除坏账计提本金）。<br />
               |新版总资产（279,913.69元）=老版总资产（306,183.62元）
               |                                        - 待收收益（19,510.20元）
               |                                        - 坏账计提本金（5,962.11元）
               |                                        - 坏账利息（493.61元）
               |                                        - 逾期利息（304.01元）
               |                                          【实时数据】<br />
               |                                    </div>
               |                                    <div class="tag_first">
               |                                           <div class="tag_first_tit">总资产统计更精准啦！</div>
               |                                           <div class="tag_first_cont">新版总资产（279,913.69元）=
               |                                        老版总资产（306,183.62元）-
               |                                        待收收益（19,510.20元）-
               |                                        坏账计提本金（5,962.11元）-
               |                                        坏账利息（493.61元）-
               |                                        逾期利息（304.01元）
               |                                        【实时数据】
               |                                           </div>
               |                                           <input type="button" value="我知道啦" class="tag_first_btn" onclick="closeReminder()" />
               |                                    </div>
               |                            </span>
               |                            </div>
               |                            <p class="data_all">¥ 279,913.69</p>
               |                        </div>
               |                        <div id="container" class="my-charts"></div>
               |                    </div>
               |                    <div class="charts-data fl">
               |                        <div class="charts-data-item fl">
               |                            <a href="http://invest.ppdai.com/myproduct/orderlist?producttype=ppb&productstatus=end" target="_blank">
               |                                <span class="p1">彩虹计划</span>
               |                                <span class="p2">¥ 0.00</span>
               |                            </a>
               |                        </div>
               |                        <div class="charts-data-item fl">
               |                            <a href="http://rise.invest.ppdai.com/account/Lend" target="_blank">
               |                                <span class="p1">月月涨</span>
               |                                <span class="p2">¥ 0.00</span>
               |                            </a>
               |                        </div>
               |                        <div class="charts-data-item mr0 fl">
               |                            <a href="http://product.invest.ppdai.com/userasset/info" target="_blank">
               |                                <span class="p1">拍活宝</span>
               |                                <span class="p2">¥ 0.00</span>
               |                            </a>
               |                            <a href="http://product.invest.ppdai.com/AssetTransfer/Product" target="_blank">
               |                                <span class="p3">收益特权</span>
               |                            </a>
               |                        </div>
               |                        <div class="charts-data-item fl">
               |                            <a href="http://invest.ppdai.com/userListingstatistics/listingstatistics" target="_blank">
               |                                <span class="p1">
               |                                    散标<span class="airBubble">
               |                                        <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                        <div class="tag">
               |                                            <div class="arrow"><em></em><span></span></div>
               |                                            当前持有本金已扣除坏账计提本金
               |                                        </div>
               |                                    </span>
               |                                </span>
               |                                <span class="p2">¥ 272,555.47</span>
               |                            </a>
               |                            <a href="http://invest.ppdai.com/AutoBidManage/StrategyList" target="_blank">
               |                                <span class="p3">自动投标(关)</span>
               |                            </a>
               |                        </div>
               |                        <div class="charts-data-item fl">
               |                            <a href="http://invest.ppdai.com/negotiable/applynew" target="_blank">
               |                                <span class="p1">债转标
               |                                    <span class="airBubble">
               |                                        <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                        <div class="tag">
               |                                            <div class="arrow"><em></em><span></span></div>
               |                                            当前持有本金已扣除坏账计提本金
               |                                        </div>
               |                                    </span>
               |                                </span>
               |                                <span class="p2">¥ 0.00</span>
               |                            </a>
               |                        </div>
               |                        <div class="charts-data-item mr0 fl">
               |                            <span class="p1">其它</span>
               |                            <span class="p2">¥ 0.00</span>
               |
               |                        </div>
               |                        <!--<div class="charts-data-item mr0 fl">-->
               |                        <!--<a href="http://invest.ppdai.com/myproduct/orderlist?producttype=newuser"-->
               |                        <!--target="_blank"><span class="p1">新手特供</span>-->
               |                        <!--<span class="p2">¥ 4265250.00</span>-->
               |                        <!--</a>-->
               |                        <!--</div>-->
               |                        <div class="charts-data-item  fl">
               |                            <span class="p1">可用余额</span>
               |                            <span class="p2">¥ 7,358.22</span>
               |                        </div>
               |                        <div class="charts-data-item fl">
               |                            <a href="http://invest.ppdai.com/ListingInvest/GoingListingNew" target="_blank">
               |                                <span class="p1">投标中金额</span>
               |                                <span class="p2">¥ 0.00</span>
               |                            </a>
               |                        </div>
               |                        <div class="charts-data-item mr0 fl">
               |                            <span class="p1">提现中金额</span>
               |                            <span class="p2">¥ 0.00</span>
               |
               |                        </div>
               |                    </div>
               |                </div>
               |            </div>
               |            <!-- 收益概况 end-->
               |            <!-- 本月回款记录  start -->
               |            <div>
               |                <div class="pro_type">
               |                    <span class="active">产品</span>
               |                    <span>散标和债转标</span>
               |                    <label class="fr titletext ">数据统计：实时</label>
               |                </div>
               |                <div class="my-ac-ctTitle clearfix selectMonthPos">
               |                    <span class="selectMonth" id="selectMonth">9月</span>
               |                    <span class="fl sp_data ml80">应收:<label id="monthReceive">¥0.00</label> </span>
               |                    <span class="fl sp_data">实收:<label id="monthRepay">¥0.00</label></span>
               |
               |                    <div class="selectbox clearfix" id="selectbox">
               |                    </div>
               |                    <div class="fr">
               |                        <a href="http://www.ppdai.com/billnew" target="_blank">
               |                            <i class="icon_bill fl"></i><span class="fl tip_link" title="电子对账单">&nbsp;电子对账单</span>
               |                            <i class="icon_bill gray"></i>
               |                        </a>
               |                    </div>
               |                    <div class="fr hide" style="margin-right:15px;display:none">
               |                        <a href="http://invest.ppdai.com/UserIncomeWaitBackDeatilsTemp/AlreadyBackDetailsPage" target="_blank">
               |                            <i class="icon_billback fl" style="background:url(http://www.ppdaicdn.com/invest/2016/img/accountNew/icon_billBack.png) no-repeat  0;width:22px;height:22px;margin-top:12px"></i><span class="fl tip_link" title="回款查询">&nbsp;回款查询</span>
               |                            <i class="icon_billback gray"></i>
               |                        </a>
               |                    </div>
               |                </div>
               |            </div>
               |            <div class="my-ac-credittotalFr c666666">
               |                <div id="calendarRight" class="my-table-sbhk"></div>
               |                <div class="ftcont clearfix">
               |
               |                    <div class="ftcont-text fl">
               |                        <p>注意</p>
               |                        <p>1.实收款显示当天应收款标的的还款情况，包含正常还款+提前还款+逾期还款;</p>
               |
               |                        <p>2.实收小于应收的情况包括：</br>
               |                            &nbsp;&nbsp;a、应还列表逾期</br>
               |                            &nbsp;&nbsp;b、赔标尚未完成赔付</br>
               |                            &nbsp;&nbsp;c、赔标借款人尚未偿还的逾期利息（根据质保计划，质保专款只赔付本金和利息，逾期利息不予赔付）
               |                        </p>
               |                    </div>
               |                </div>
               |            </div>
               |            <!-- 本月回款记录 end -->
               |            <!-- 资产情况 start-->
               |            <div class="my-ac-credittotalFr c666666">
               |                <div class="my-ac-ctTitle clearfix">
               |                    <label class="titletext fr">数据统计：实时</label>
               |                </div>
               |                <div class="my-ac-ctList clearfix">
               |                    <div class="my-ac-item fl">
               |                        <div class="my-ac-cl-one">
               |                            <span>累计净充值金额</span><span class="airBubble">
               |                                <img src="http://www.ppdaicdn.com/2014/img/question.png" />
               |                                <div class="tag">
               |                                    <div class="arrow"><em></em><span></span></div>
               |                                    累计充值金额-累计取现金额
               |                                </div>
               |                            </span>
               |                        </div>
               |                        <div class="my-ac-cl-two">
               |                            <span class="ft14">¥</span>220,660.00
               |                        </div>
               |                        <div class="my-ac-cl-three"></div>
               |                    </div>
               |                    <div class="my-ac-item fl">
               |                        <div class="my-ac-cl-one">
               |                            <span>累计充值金额</span>
               |                        </div>
               |                        <div class="my-ac-cl-two">
               |                            <a href="http://www.ppdai.com/moneyhistory?Type=0&Time=3" target="_blank"><span class="ft14">¥</span>220,666.00</a>
               |                        </div>
               |                        <div class="my-ac-cl-three">
               |                        </div>
               |                    </div>
               |                    <div class="my-ac-item fl">
               |                        <div class="my-ac-cl-one">
               |                            累计取现金额
               |                        </div>
               |                        <div class="my-ac-cl-two">
               |                            <a href="http://www.ppdai.com/moneyhistory?Type=2&Time=3" target="_blank"><span class="ft14">¥</span>6.00</a>
               |                        </div>
               |                    </div>
               |                </div>
               |            </div>
               |            <!-- 资产情况 end-->
               |        </div>
               |        <!-- 右侧 end-->
               |        <input type="hidden" id="serveryear" value="2017" />
               |        <input type="hidden" id="servermonth" value="9" />
               |        <input type="hidden" id="serverday" value="18" />
               |    </div>
               |</div>
               |""".stripMargin
//    val start=html.indexOf("正常还清次数")
//    if(start < 0) return false
//    def getExpNum(exp:String)={
//      val cutStart=html.indexOf(exp)
//      html.substring(cutStart+exp.length+1,html.indexOf("</p>",cutStart)).replace("""<span class="num">""","").replace("</span>","").replace("次","").trim()
//    }
//    val List(count,yu,hei)=List("正常还清次数","逾期(0-15天)还清次数","逾期(15天以上)还清次数").map(v=>getExpNum(v).toInt)
//    val l=new LoanData().queryById("683054")
//    System.exit(0)
//    OtsCache.setCache("nimei","你妹".getBytes)
//    println(new String(OtsCache.getCache("nimei").get))
//    val all=OtsCache.getAll
//    all.foreach{d=>
//      d.getColumns
//      println(d.getPrimaryKey.getPrimaryKeyColumns.head.getValue.asString())
//
//    }
//    println(all.size)
//    println(new Borrow(returnDate = new Date()).createTable())
//    val data=File2Byte(new File("z:\\Image.gif"))
//      println(Image.getImageCode(data))
//    val lid="20661086"
//    val fullData=new LoanText().queryOne("ListingId=?",lid).map(_.text).getOrElse("")
//    val cutStart=fullData.indexOf("<p>正常还清")
//    val txt=fullData.substring(cutStart+3,fullData.indexOf("</p>",cutStart))
//    val List(count,yu,hei)=txt.split("，").map(v=> v.split("：").last.trim.dropRight(1)).toList
//    println(count)
//    println(yu)
//    println(hei)
//    println(List(1,6,3,4,5).sortBy(v=> v * -1))
//    new Setting(-1,"","","").insert()
//    new Bid().createTable()
//    println("-2d".dateExp.sdate)
//    val entity = new ByteArrayEntity("""{"Borrowernumber":"24069614","UserId":0,"listingId":"19129600"}""".getBytes("UTF-8"))
//    val v=NetTool.HttpPost("http://wirelessgateway.ppdai.com/Invest/BorrowerinfoService/Borrowerinfo",entity=entity)
//    println(v._2)
//    PaiPaiLoans.loanInfo(19286556)
  }


  def runTasks(){
    var two=0
    val set=new mutable.HashMap[Int,Int]()
    var hour=8
    var minute=27
    var second=17
    var t=TimeTool.parseStringToDateTime("2017-4-1 00:00:00").getTime
    val tasks=new Task().query("status=0 and id not in (1,4,5)")
    println(tasks.toJson())
    while (true){
      val date=new Date(t)
      if(date.getHours==0 && date.getMinutes==0 && date.getSeconds ==0){ //清除当日执行队列
        set.clear()
        hour=7+Random.nextInt(13)
        minute=10+Random.nextInt(48)
        second=10+Random.nextInt(45)
      }
      val i=t/1000
      tasks.foreach{t=>
        val List(ttype,value)=t.time.split(",").toList
        val isMatch=ttype match{
          case "t"=>  i% value.toInt ==0
          case "rd"=>
            if(!set.contains(t.id)){
              val h=value.split(":").head.toInt +Random.nextInt(value.split(":").last.toInt)
              set(t.id)=h
            }
            val ret=date.getHours==set(t.id)&& date.getMinutes==minute && date.getSeconds ==second
            if(ret) {
              second = second - Random.nextInt(3)
              minute = minute - Random.nextInt(3)
            }
            ret
          case "rm"=>
            if(!set.contains(t.id)){
              val d=value.split(":").head.toInt +Random.nextInt(value.split(":").last.toInt)
              set(t.id)=d
            }
            val ret=date.getDate==set(t.id)&&date.getHours==hour&& date.getMinutes==minute && date.getSeconds ==second
            if(ret) {
              second = second - Random.nextInt(3)
              minute = minute - Random.nextInt(3)
              hour = hour - Random.nextInt(3)
            }
            ret
          case "d"=> date.sdate == value
          case v:Any=> println("unsuper:"+v)
            false
        }
        if(isMatch){
//          run {
            val cl = Class.forName(t.method.split("\\.").dropRight(1).mkString(".")).newInstance()
            val me = cl.getClass.getMethod(t.method.split("\\.").last)
            try {
              val ret = me.invoke(cl)
              new TaskLog(0, t.id, t.time, 0, ret.toString, new Date()).insert()
            } catch {
              case ex: Throwable =>
                ex.printStackTrace()
                new TaskLog(0, t.id, t.time, 1, ex.getMessage, new Date()).insert()
            }
//          }
          two=two +1
        }
        if(two==2) {
          System.exit(0)
        }
      }
      t = t+ 1000
    }
  }
}

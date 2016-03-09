package common

/**
 * Created by 林 on 14-3-27.
 */
class VenusException(msg: String) extends Exception(msg) {
  //更多的详情
  val detail: String = ""
}

class NoUserExcepiton extends VenusException("用户不存在")

class LoginFailException extends VenusException("账号或密码错误")

class PhoneExistExcepiton extends VenusException("手机号已经被注册")

class UnSupportQueryExcepiton(val ref: Any) extends VenusException("请求对象未定义")

class UnSupportExcepiton extends VenusException("不支持的操作，赶紧反馈给程序猿")

class EmptyFieldExcepiton extends VenusException("缺少必要数据")

class DataOverExcepiton extends VenusException("当前访问数据不是你的")

class DataNoFindExcepiton extends VenusException("访问的数据不存在")

class NoPassException extends VenusException("验证失败")

class PhoneErrorExcepiton extends VenusException("手机号错误")

class RequstLimitExcepiton extends VenusException("操作频率限制，请稍候再试")

class NeedLoginExcepiton extends VenusException("需要登录才能访问")

class ForbidenExcepiton extends VenusException("你已经被禁止登录，请联系管理员解封")

class ArticleUseExcepiton extends VenusException("文章已经被转换,不允许删除,请使用暂停或者替换功能操作")

class UrlErrorExcepiton extends VenusException("链接不符合要求,请使用转换后文章链接")

class delColumnException extends VenusException("此栏目有文章，不可删除栏目")

class delAdminumnException extends VenusException("此管理员已对文章有过处理，不可删除管理员")

class TimeException extends VenusException("开始时间已经在结束时间之后 不符合要求")

class UnSupportPcExcepiton extends VenusException("禁止pc注册")

class AdminNoInitExcepiton extends VenusException("管理员还没初始化，无任何权限")

class ProductSellOutExcepiton extends VenusException("商品已售完")

class ProductInsufficientExcepiton extends VenusException("商品数量不足")

class CouponNoUseExcepiton extends VenusException("优惠券不可用")
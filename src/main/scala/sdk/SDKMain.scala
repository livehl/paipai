package sdk

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import common.Tool._
import db.BackList

/**
  * Created by admin on 2016/9/9.
  */
object SDKMain {
  lazy val conf = ConfigFactory.load()
  def aiUrl=conf.getString("aiurl")
  def main(args: Array[String]) {
    UserApi.updateUsers
    run(Task.runTasks())
    //启动消息发送Actor
    val system = ActorSystem.create("PaiPai")
    val userActor = system.actorOf(Props[UserActor], name = "user_actor")
    val loanActor = system.actorOf(Props(new LoanActor(userActor)), name = "loan_actor")
    LoansApi.loanActor(loanActor)
  }

}

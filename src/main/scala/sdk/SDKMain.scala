package sdk

import akka.actor.{ActorSystem, Props}
import common.Tool._

/**
  * Created by admin on 2016/9/9.
  */
object SDKMain {
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

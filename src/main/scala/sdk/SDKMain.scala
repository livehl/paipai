package sdk

import akka.actor.{ActorSystem, Props}
import common.Tool._
import main.{PaiPaiLoans, PaiPaiUser}

/**
  * Created by admin on 2016/9/9.
  */
object SDKMain {
  def main(args: Array[String]) {
    PaiPaiUser.updateUsers
    run(PaiPaiLoans.collectLoan)
    run(PaiPaiUser.collectUser)
    //启动消息发送Actor
    val system = ActorSystem.create("PaiPai")
    val userActor = system.actorOf(Props[UserActor], name = "user_actor")
    val loanActor = system.actorOf(Props(new LoanActor(userActor)), name = "loan_actor")
    Loans.loanActor(loanActor)
  }

}

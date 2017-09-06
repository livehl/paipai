package sdk

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import common.Tool
import common.Tool._
import db.{BackList, Setting}

/**
  * Created by admin on 2016/9/9.
  */
object SDKMain {
  lazy val conf = ConfigFactory.load()
  def main(args: Array[String]) {
    UserApi.updateUsers
    run(Task.runTasks)
    run(updateSetting)
    //启动消息发送Actor
    val system = ActorSystem.create("PaiPai")
    val userActor = system.actorOf(Props[UserActor], name = "user_actor")
    val loanActor = system.actorOf(Props(new LoanActor(userActor)), name = "loan_actor")
    LoansApi.loanActor(loanActor)
  }
  def updateSetting()= {
    while (true) {
      safe {
        val settingMap = new Setting().queryAll.map(s => s.name -> s.value).toMap
        Tool.setSetting(settingMap)
        Thread.sleep(1000*30)
      }
    }
  }

}

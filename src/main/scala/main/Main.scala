package main
import common.Tool._
/**
  * Created by admin on 2016/9/9.
  */
object Main {
  def main(args: Array[String]) {
    safe {
      PaiPaiUser.updateUsers
    }
    run(PaiPaiLoans.collectLoan)
    run(PaiPaiUser.collectUser)
    PaiPaiLoans.loanStream()
  }

}

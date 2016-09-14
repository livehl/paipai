package main
import common.Tool._
/**
  * Created by admin on 2016/9/9.
  */
object Main {
  def main(args: Array[String]) {
    run(PaiPaiLoans.collectLoan)
    PaiPaiUser.collectUser
  }

}

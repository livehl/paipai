package tools

import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail._
import javax.mail._
import java.util.Date

/**
 * Created by 林 on 14-4-11.
 */
class EMail(email: String, password: String, smtp: String) {
  val session = getSession
  def sendMail(address:String,title:String,body:String)={
    try{
      val msg = new MimeMessage(session)
      msg.setFrom(new InternetAddress(email))
      msg.setRecipients(Message.RecipientType.TO, address)
      msg.setSubject(title)
      msg.setSentDate(new Date())
      if (body.contains("<") && body.contains(">")) {
        msg.setContent(body, "text/html;charset=utf-8")
      } else {
        msg.setText(body)
      }
      Transport.send(msg)
      true
    }catch {
      case th:Throwable=>th.printStackTrace()
        false
    }
  }

  private def getSession() = {
    val SSL_FACTORY = "javax.net.ssl.SSLSocketFactory"
    val props = System.getProperties()
    props.setProperty("mail.smtp.host", smtp)
    props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY)
    props.setProperty("mail.smtp.socketFactory.fallback", "false")
    props.setProperty("mail.smtp.port", "465")
    props.setProperty("mail.smtp.socketFactory.port", "465")
    props.put("mail.smtp.auth", "true")
    val ah = new Authenticator() {
      override protected def getPasswordAuthentication(): PasswordAuthentication = {
          new PasswordAuthentication(email, password)
      }
    }
    Session.getDefaultInstance(props, ah)
  }

}

package services

import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

object EmailService {

  private val mailer: Mailer = MailerBuilder
    .withSMTPServer(System.getProperty("SMTP_HOST"), System.getProperty("SMTP_PORT").toInt, System.getProperty("MAIL_USERNAME"), System.getProperty("MAIL_PASSWORD"))
    .withTransportStrategy(TransportStrategy.SMTP_TLS)
    .buildMailer()

  def sendEmail(toEmails: Seq[String], subject: String, bodyText: String): Unit = {
    val email = EmailBuilder.startingBlank()
      .from("System", System.getProperty("MAIL_FROM_EMAIL"))
      .withSubject(subject)
      .withPlainText(bodyText)

    // Add all recipients to the email
    toEmails.foreach(email.to("Recipient", _))

    // Build the email and send it
    mailer.sendMail(email.buildEmail())
  }
}

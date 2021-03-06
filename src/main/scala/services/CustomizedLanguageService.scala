package services

import javax.inject.Inject
import play.api.i18n.{Lang, MessagesApi}

class CustomizedLanguageService @Inject()(messagesApi: MessagesApi)  {

  def customizedLanguageMessage(language: String, messageCode: String): String = {
    messagesApi(messageCode)(Lang(language))
  }

  def customizedLanguageMessage(language: String, messageCode: String, args1: String): String = {
    messagesApi(messageCode, args1)(Lang(language))
  }

  def customizedLanguageMessage(language: String, messageCode: String, args1: String, args2: String): String = {
    messagesApi(messageCode, args1, args2)(Lang(language))
  }

  def customizedLanguageMessage(language: String, messageCode: String, args1: String, args2: String, args3: String): String = {
    messagesApi(messageCode, args1, args2, args3)(Lang(language))
  }

  def allMessages: String = {
    val values : Map[String, Map[String, String]] = messagesApi.messages

    val stringBuilder: StringBuilder = new StringBuilder

    values.map { value =>
      stringBuilder.append(value._1 + " = " + "\n")

      value._2.map { mapValues =>
        stringBuilder.append(mapValues._1 + " " + mapValues._2)
        stringBuilder.append("\n")
      }
//      stringBuilder.append(value._2.mkString)
      stringBuilder.append("\n\n\n")
    }

    stringBuilder.toString()
  }
}
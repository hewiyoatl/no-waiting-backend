package controllers

import formatter._
import javax.inject.Inject
import models.{Addresses, Restaurant, RestaurantModel, Restaurants}
import org.joda.time.DateTime
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.{CustomizedLanguageService, LanguageAction, MetricsService}
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class RestaurantController @Inject()(cc: ControllerComponents, restaurants: Restaurants, addressService: Addresses)
                                    (implicit context: ExecutionContext,
                                     metricsService: MetricsService,
                                     interMessage: CustomizedLanguageService,
                                     languageAction: LanguageAction,
                                     util: Util) extends AbstractController(cc) {

  implicit val restaurantReader = RestaurantFormatter.RestaurantReader

  implicit val restaurantWriter = RestaurantFormatter.RestaurantWriter

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  implicit val successWriter = SuccessMessageFormatter.successWriter

  def listRestaurants = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    restaurants.listAll map { restaurants =>
      val listRestaurants:Option[String] = Some(Json.stringify(Json.toJson(restaurants)))
      val message: String = interMessage.customizedLanguageMessage(language, "restaurant.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listRestaurants))).withHeaders(util.headersCors: _*)
    }
  }

  def addRestaurant = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[Restaurant] = json.validate[Restaurant]

        resultVal.asOpt.map { restaurantInboud =>

          var addressId: Option[Long] = addressService.retrieveAddressPerPrimaryKey(Some(restaurantInboud.addressInfo)).map(_.id).getOrElse(None)
          //if the address was not there in the table,
          //but user wants to update his/her address
          if (addressId.getOrElse(0) == 0 && addressService.receivingPrimaryKeyQuestion(Some(restaurantInboud.addressInfo))) {
            val addressResult: Option[Long] = addressService.add(restaurantInboud.addressInfo)
            addressId = addressResult

            val newRestaurant = RestaurantModel(
              None,
              restaurantInboud.businessName,
              restaurantInboud.phoneNumber,
              restaurantInboud.averageWaitingTime,
              addressId.get,
              None,
              None,
              false)

            restaurants.add(newRestaurant) map { restaurant =>
              val restaurantString:Option[String] = Some(Json.stringify(Json.toJson(restaurant)))
              val message: String = interMessage.customizedLanguageMessage(language, "restaurant.creation.success", "")
              Created(Json.toJson(SuccessMessage(OK, message, restaurantString))).withHeaders(util.headersCors: _*)
            }

          } else {
            val message: String = interMessage.customizedLanguageMessage(language, "restaurant.creation.error", "")
            Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
          }

        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "restaurant.creation.error", "")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "restaurant.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def deleteRestaurant(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    restaurants.delete(id)
    val message: String = interMessage.customizedLanguageMessage(language, "restaurant.delete.success", "")
    Future(Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*))
  }

  def retrieveRestaurant(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    restaurants.retrieveRestaurant(id) map { restaurant =>
      val restaurantString:Option[String] = Some(Json.stringify(Json.toJson(restaurant)))
      val message: String = interMessage.customizedLanguageMessage(language, "restaurant.retrieve.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, restaurantString))).withHeaders(util.headersCors: _*)
    }
  }

  def patchRestaurant(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[Restaurant] = json.validate[Restaurant]

        resultVal.asOpt.map { restaurantInboud =>

          var addressId: Option[Long] = addressService.retrieveAddressPerPrimaryKey(Some(restaurantInboud.addressInfo)).map(_.id).getOrElse(None)
          //if the address was not there in the table,
          //but user wants to update his/her address
          if (addressId.getOrElse(0) == 0 && addressService.receivingPrimaryKeyQuestion(Some(restaurantInboud.addressInfo))) {
            val addressResult: Option[Long] = addressService.add(restaurantInboud.addressInfo)
            addressId = addressResult
          }

          val patchRestaurant = RestaurantModel(
            Some(id),
            restaurantInboud.businessName,
            restaurantInboud.phoneNumber,
            restaurantInboud.averageWaitingTime,
            addressId.get,
            None,
            Some(DateTime.now()),
            false)

          restaurants.patchRestaurant(patchRestaurant) map { restaurant =>
            val restaurantString:Option[String] = Some(Json.stringify(Json.toJson(restaurant)))
            val message: String = interMessage.customizedLanguageMessage(language, "restaurant.update.success", "")
            Ok(Json.toJson(SuccessMessage(OK, message, restaurantString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "restaurant.update.error", "")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "restaurant.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
    }
  }
}

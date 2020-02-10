package controllers

import formatter._
import javax.inject.Inject
import models.{Restaurant, Restaurants}
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.MetricsService
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class RestaurantController @Inject()(cc: ControllerComponents, restaurants: Restaurants)
                                    (implicit context: ExecutionContext,
                                     metricsService: MetricsService,
                                     util: Util) extends AbstractController(cc) {

  implicit val restaurantReader = RestaurantFormatter.RestaurantReader

  implicit val restaurantWriter = RestaurantFormatter.RestaurantWriter

  implicit val errorWriter = ErrorFormatter.errorWriter

  implicit val successWriter = SuccessFormatter.successWriter

  def listRestaurants = Action.async { implicit request =>
    restaurants.listAll map { restaurants =>
      val listRestaurants:Option[String] = Some(Json.stringify(Json.toJson(restaurants)))
      Ok(Json.toJson(Success(OK, "List of restaurants", listRestaurants))).withHeaders(util.headersCors: _*)
    }
  }

  def addRestaurant = Action.async { implicit request =>

    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[RestaurantInbound] = json.validate[RestaurantInbound]

        resultVal.asOpt.map { restaurantInboud =>

          val newRestaurant = Restaurant(
            None,
            restaurantInboud.address1,
            restaurantInboud.address2,
            restaurantInboud.zipCode,
            restaurantInboud.state,
            restaurantInboud.city,
            restaurantInboud.country,
            restaurantInboud.phoneNumber,
            restaurantInboud.restaurantId,
            restaurantInboud.latitude,
            restaurantInboud.longitude,
            None,
            false)

          restaurants.add(newRestaurant) map { restaurant =>
            val restaurantString:Option[String] = Some(Json.stringify(Json.toJson(restaurant)))
            Created(Json.toJson(Success(OK, "Restaurant created successfully", restaurantString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Validation of fields failed."))))
        }
    } getOrElse {
      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Request body not completed"))))
    }
  }

  def deleteRestaurant(id: Long) = Action.async { implicit request =>
    restaurants.delete(id)
    Future(Ok(Json.toJson(Success(OK, "Your restaurant was deleted correctly", None))).withHeaders(util.headersCors: _*))
  }

  def retrieveRestaurant(id: Long) = Action.async { implicit request =>
    restaurants.retrieveRestaurant(id) map { restaurant =>
      val restaurantString:Option[String] = Some(Json.stringify(Json.toJson(restaurant)))
      Ok(Json.toJson(Success(OK, "Restaurant retrieve successfully", restaurantString))).withHeaders(util.headersCors: _*)
    }
  }

  def patchRestaurant(id: Long) = Action.async { implicit request =>

    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[RestaurantInbound] = json.validate[RestaurantInbound]

        resultVal.asOpt.map { restaurantInboud =>

          val patchRestaurant = Restaurant(
            Some(id),
            restaurantInboud.address1,
            restaurantInboud.address2,
            restaurantInboud.zipCode,
            restaurantInboud.state,
            restaurantInboud.city,
            restaurantInboud.country,
            restaurantInboud.phoneNumber,
            restaurantInboud.restaurantId,
            restaurantInboud.latitude,
            restaurantInboud.longitude,
            None,
            false)

          restaurants.patchRestaurant(patchRestaurant) map { restaurant =>
            val restaurantString:Option[String] = Some(Json.stringify(Json.toJson(restaurant)))
            Ok(Json.toJson(Success(OK, "Restaurant updated successfully", restaurantString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "There was an error updating the restaurant"))))
        }
    } getOrElse {
      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Invalid request body"))))
    }
  }
}

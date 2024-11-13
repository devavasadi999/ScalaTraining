package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.Inject

class StoreController @Inject()(val controllerComponents: ControllerComponents) extends BaseController{
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.store())
  }
  def report() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.report())
  }
}

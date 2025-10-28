/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.iossintermediaryregistrationstub.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.iossintermediaryregistrationstub.connectors.RegistrationConnector
import uk.gov.hmrc.iossintermediaryregistrationstub.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossintermediaryregistrationstub.models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers, SubscriberRequest}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject

class EnrolmentsController @Inject()(
                                      cc: DefaultAuthenticatedControllerComponents,
                                      registrationConnector: RegistrationConnector,
                                      clock: Clock
                                    ) extends BackendController(cc) with Logging {

  def confirm(subscriptionId: String): Action[SubscriberRequest] = Action(parse.json[SubscriberRequest]) {
    implicit request =>
      if (request.headers.headers.exists(_._1.equalsIgnoreCase(AUTHORIZATION))) {
        if (subscriptionId.startsWith("666000003-id")) {
          BadRequest
        } else if (subscriptionId.startsWith("666000004-id")) {
          Unauthorized
        } else {
          logger.info(s"Call back URL: ${request.body.callback}")
          registrationConnector.doCallback(subscriptionId, request.body.callback)
          NoContent
        }
      }
      else {
        BadRequest(Json.toJson(s"Bad Request - missing $AUTHORIZATION"))
      }
  }

  def es2(userId: String): Action[AnyContent] = cc.auth() { request =>
    val enrolmentsToReturn = request.iossNumber match {
      case Some("IM9007230000") =>
        EACDEnrolments(Seq(
          EACDEnrolment("HMRC-IOSS-ORG", "Activated", Some(LocalDateTime.now(clock).minusMonths(6)), Seq(EACDIdentifiers("IOSSNumber", "IM9006230000"))),
          EACDEnrolment("HMRC-IOSS-ORG", "Activated", Some(LocalDateTime.now(clock).minusMonths(3)), Seq(EACDIdentifiers("IOSSNumber", "IM9007230000")))
        ))
      case _ =>
        EACDEnrolments(Seq(EACDEnrolment("HMRC-IOSS-ORG", "Activated", Some(LocalDateTime.now(clock)), Seq(EACDIdentifiers("IOSSNumber", request.iossNumber.get)))))
    }
    Ok(Json.toJson(enrolmentsToReturn))
  }

}

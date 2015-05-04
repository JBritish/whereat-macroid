package org.tlc.whereat.services

import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.internal.result.Parser
import org.specs2.mutable.Specification
import org.tlc.whereat.model.{ApiIntersection, Intersection, Loc}
import org.tlc.whereat.msg.IntersectionResponse
import org.tlc.whereat.support.{AppContextTestSupport, BaseTestSupport, AsyncUtils}

import scala.concurrent.Future

/**
 * Author: @aguestuser
 * Date: 5/3/15
 * License: GPLv2 (https://www.gnu.org/licenses/gpl-2.0.html)
 */

trait IntersectionServiceMock
  extends IntersectionService
  with AppContextTestSupport
  with AsyncUtils {

  val rcLoc = Loc(40.7206235,-74.0007963)
  val rcLocReq = toIntersectionRequest(rcLoc)

  val validIntersection = ApiIntersection (
    lat = "40.72084",
    lng = "-74.000661",
    distance = "0.03",
    street1 = "Broadway",
    street2 = "Grand St",
    street1Bearing = "32",
    street2Bearing = "124",
    placename = "New York",
    adminName1 = "New York",
    adminName2 = "New York",
    adminCode1 = "NY",
    postalcode = "10013",
    countryCode = "US",
    mtfcc1 = "S1400",
    mtfcc2 = "S1400"
  )

}

class IntersectionServiceTest extends Specification with BaseTestSupport {

  "Intersection Service" should {

    "parse intersection from JSON" >> new IntersectionServiceMock {

      override def reqJson[T](url: String)(implicit parser: Parser[T], client: OkHttpClient = new OkHttpClient()): Future[T] =
        Future.successful[T](validIntersection.asInstanceOf[T])

//      Await.result(IntersectionService.getIntersection(rcLocReq), Duration.Inf) ===
      IntersectionService.getIntersection(rcLocReq) *===
        IntersectionResponse(Some(Intersection(street1 = "Grand St", street2 = "Broadway")))

    }

  }

}

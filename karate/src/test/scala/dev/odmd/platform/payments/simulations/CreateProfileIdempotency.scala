package com.ondemand.platform.springcdk.simulations

import com.intuit.karate.gatling.KarateProtocol
import com.intuit.karate.gatling.PreDef._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import java.util.UUID.randomUUID
import scala.concurrent.duration._
import scala.language.postfixOps

class CreateProfileIdempotency extends Simulation {
  val protocol: KarateProtocol = karateProtocol(
    "/payment-profiles" -> Nil
  )

  val numGroups = 10
  val usersPerGroup = 10

  val scenarios: Iterator[ScenarioBuilder] = Iterator
    // iterator generates unique request & customer ID pairs
    .continually(Map("requestId" -> randomUUID.toString, "customerId" -> randomUUID.toString))
    // grab the amount we need
    .take(numGroups)
    // create scenario which executes the karate feature using the generated request & customer IDs
    // where each "virtual user" sends the same request & customer ID
    .map { group =>
      scenario(s"request ${group.get("requestId")}")
        .feed(Iterator.continually(group))
        .exec(karateFeature("classpath:profile/createPaymentProfilePerf.feature"))
    }

  setUp(
    scenarios.map { scenario =>
      // for each scenario, inject the desired number of concurrent users making the same request
      scenario
        .inject(atOnceUsers(usersPerGroup))
        .protocols(protocol)
    }.toList
  ).assertions(
    global.successfulRequests.percent.is(100)
  )
}

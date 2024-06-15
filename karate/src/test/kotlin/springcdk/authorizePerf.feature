@perf
Feature: Auth Capture Idempotency

  Background:
    * url urlBase
    * def dateResponse = call read('classpath:helpers/getDate.feature')
    * def expMonth = parseInt(dateResponse.currentMonth)
    * def expYear = parseInt(dateResponse.currentYear)
    * def uuid = function(){ return java.util.UUID.randomUUID().toString() }
    * def shortUUID = function(){ return java.util.UUID.randomUUID().toString().split('-')[0] }

    * def targetKey = __gatling.targetKey
    * def requestId = __gatling.requestId
    * def customerId = __gatling.customerId
    * def lineItemId = __gatling.lineItemId
    * configure retry = { count: 10, interval: 200 }
    * configure headers = { odmd-eee-id: '#(customerId)', odmd-aakey: '#(xxxApiKey)' }

  Scenario: Create Authorize Payments - Auth & Capture flow
    ####################################
    # Arrange
    ####################################
    # Set Authorize Payment Request
    * def shouldCapture = true
    * def paymentAuthorizePayload = read('classpath:payloads/createAuthorizePayment.json')

    Given path '/springcdk/authorize'
    And header odmd-payment-gateway-id = 'WORLDPAY'
    And request paymentAuthorizePayload
    And retry until responseStatus == 200
    When method post
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response.requestId == requestId
    * def payment = response.springcdk[0]
    * match payment.status == "SUCCESS"
    * match payment.authorized == paymentAuthorizePayload.paymentDtos[0].authorizeAmount
    * match payment.captured == paymentAuthorizePayload.paymentDtos[0].shouldCapture
    * match payment.profileId ==  '#notnull'

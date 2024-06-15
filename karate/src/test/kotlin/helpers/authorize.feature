# Helper to authorize a single payment:
#
# param: customerId    - Required. Used as the customer that owns created profile & payment.
# param: shouldCapture - Optional. Determines whether to auth or auth+capture (defaults to auth).
#
@ignore
Feature: Payment-Platform API Tests - Payments Controller
  Background:
    * url urlBase
    * def dateResponse = call read('classpath:helpers/getDate.feature')
    * def expMonth = parseInt(dateResponse.currentMonth)
    * def expYear = parseInt(dateResponse.currentYear)
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }
    * configure headers = { odmd-eee-id: '#(customerId)', odmd-aakey: '#(xxxApiKey)' }

  Scenario: Create payment
    ####################################
    # Arrange
    ####################################
    # Set Authorize Payment Request
    * def requestId = uuid()
    * def targetKey = uuid()
    # If an object is passed as an argument and it has a shouldCapture property, use it.
    * def lineItemId = typeof (__arg || {}).lineItemId == "undefined" ? uuid() : __arg.lineItemId
    * def shouldCapture = typeof (__arg || {}).shouldCapture == "undefined" ? "false" : __arg.shouldCapture
    * def customerId = typeof (__arg || {}).customerId == "undefined" ? uuid() : __arg.customerId
    * def paymentMethod = __arg.paymentMethod || read('classpath:payloads/creditCardPaymentMethod.json')
    * def paymentAuthorizePayload = read('classpath:payloads/createAuthorizePayment.json')

    Given path '/springcdk/authorize'
    And request paymentAuthorizePayload
    When method post
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response.requestId == requestId
    * match response.springcdk[0].status == "SUCCESS"


@e2e
Feature: Payment-Platform API Tests - Payments Controller

  Background:
    * url urlBase
    * def dateResponse = call read('classpath:helpers/getDate.feature')
    * def expMonth = parseInt(dateResponse.currentMonth)
    * def expYear = parseInt(dateResponse.currentYear)
    * def uuid = function(){ return java.util.UUID.randomUUID().toString() }
    * def paymentMethod = read('classpath:payloads/creditCardPaymentMethod.json')
    # HACK: we currently set customerReference to targetKey, which WorldPay restricts to 17 characters, too short for a full-length UUID
    * def shortUUID = function(){ return java.util.UUID.randomUUID().toString().split('-')[0] }
    * configure headers = { odmd-aakey: '#(xxxApiKey)' }

  Scenario Outline: Create Authorize Payments - Auth & Capture flow
    ####################################
    # Arrange
    ####################################
    # Set Authorize Payment Request
    * def shouldCapture = true
    * def targetKey = shortUUID()
    * def customerId = uuid()
    * def requestId = uuid()
    * def lineItemId = uuid()

    * def paymentAuthorizePayload = read('classpath:payloads/createAuthorizePayment.json')

    Given path '/springcdk/authorize'
    And header odmd-payment-gateway-id = '<gateway>'
    And header odmd-eee-id = customerId
    And request paymentAuthorizePayload
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


    * def paymentProfile = response.paymentProfiles[0]
    * match paymentAuthorizePayload.paymentDtos[0].paymentMethod.creditCard.billingInformation.email == paymentProfile.paymentMethod.savedCreditCard.email

    Examples:
      |gateway|
      |WORLDPAY|
      |STRIPE  |

  Scenario Outline: Create Authorize Payments - Auth Only flow
    ####################################
    # Arrange
    ####################################
    # Set Authorize Payment Request
    * def shouldCapture = false
    * def targetKey = shortUUID()
    * def customerId = uuid()
    * def requestId = uuid()
    * def lineItemId = uuid()
    * def paymentAuthorizePayload = read('classpath:payloads/createAuthorizePayment.json')

    Given path '/springcdk/authorize'
    And header odmd-payment-gateway-id = '<gateway>'
    And header odmd-eee-id = customerId
    And request paymentAuthorizePayload
    When method post
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response.requestId == requestId
    * def payment = response.springcdk[0]
    * match payment.status == "SUCCESS"
    * match payment.captured == paymentAuthorizePayload.paymentDtos[0].shouldCapture

    Examples:
      |gateway|
      |WORLDPAY|
      |STRIPE  |

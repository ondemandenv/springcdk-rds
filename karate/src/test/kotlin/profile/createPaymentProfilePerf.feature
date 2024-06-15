@perf
Feature: Create Payment Profile Idempotency

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }
    * def requestId = __gatling.requestId
    * def customerId = __gatling.customerId
    * configure retry = { count: 10, interval: 200 }

  Scenario: Create Payment Profile

    ####################################
    # Arrange
    ####################################

    # Set Payment Profile Request
    * def cardNumber = '4111111111111111'

    * def paymentProfilePayload = read('classpath:payloads/createPaymentProfile.json')
    * def headers = { odmd-eee-id: #(customerId), odmd-aakey: '#(xxxApiKey)' }
    # Set gateway header if defined
#    * if (__arg.gateway) headers['odmd-payment-gateway-id'] = gateway

    ####################################
    # Act
    ####################################

    Given path '/payment-profiles'
    And headers headers
    And request paymentProfilePayload
    And retry until responseStatus == 200
    When method post

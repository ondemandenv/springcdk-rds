@e2e
Feature: Payment-Platform API Tests - Payments Controller - refund Payment

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }

    * def customerId = uuid()
    * def lineItemId = uuid()
    * def result =  call read('classpath:helpers/authorize.feature') { customerId: #(customerId), shouldCapture: true, lineItemId: #(lineItemId) }
    * def paymentId = result.response.springcdk[0].paymentId
    * configure headers = { odmd-eee-id: '#(customerId)', odmd-aakey: '#(xxxApiKey)' }

  Scenario: Refund Payment By PaymentId
    ####################################
    # Arrange
    ####################################
    # Set Refund Payment Request
    * def requestId = uuid()
    * def amount = "1.89"
    * def paymentRefundPayload = read('classpath:payloads/createRefundPayment.json')

    Given path '/springcdk/' + paymentId + '/refund/'
    And request paymentRefundPayload
    When method put
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response.refundType == paymentRefundPayload.refundType
    * match response.errors == []


  Scenario: Refund Payment is more than capturedAmount - Bad Request
    ####################################
    # Arrange
    ####################################
    # Set Refund Payment Request
    * def requestId = uuid()
    * def amount = "30.89"
    * def paymentRefundPayload = read('classpath:payloads/createRefundPayment.json')

    Given path '/springcdk/' + paymentId + '/refund/'
    And request paymentRefundPayload
    When method put
    Then status 400

    ####################################
    # Assert
    ####################################
    * match response.error == "Bad Request"
    * match response.message contains "Line item " + lineItemId + " cannot be refunded"

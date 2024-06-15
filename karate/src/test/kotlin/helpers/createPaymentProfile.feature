@ignore
Feature: Helper to Create Payment Profile

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }

  Scenario: Create Payment Profile

    ####################################
    # Arrange
    ####################################

    # Set Payment Profile Request
    * def cardNumber = __arg.cardNumber || '4111111111111111'
    * def expectedStatus = __arg.status || 200
    * def email = __arg.email || 'test@example.com'
    * def requestId = uuid()
    * def paymentProfilePayload = read('classpath:payloads/createPaymentProfile.json')
    * def headers = { odmd-eee-id: #(customerId), odmd-aakey: '#(xxxApiKey)' }
    * def params = {}
    # Set gateway header if defined
    * if (__arg.gateway) headers['odmd-payment-gateway-id'] = gateway
    * if (typeof __arg.validate !== 'undefined') paymentProfilePayload['validate'] = __arg.validate

    ####################################
    # Act
    ####################################

    Given path '/payment-profiles'
    And headers headers
    And request paymentProfilePayload
    When method post
    Then match responseStatus == expectedStatus

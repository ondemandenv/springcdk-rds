@e2e
Feature: Payment-Platform API Tests - Payment Profile Controller

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }

  Scenario: Get Profile Token
    * string customerId = uuid()
    * configure headers = { odmd-eee-id: #(customerId), odmd-aakey: '#(xxxApiKey)' }

####################################
# Arrange
####################################

# Set Payment Profile Request
    * def paymentProfile = call read('classpath:helpers/createPaymentProfile.feature')  { requestId: #(requestId)}

####################################
# Act
####################################

    * def id = paymentProfile.response.paymentMethod.savedCreditCard.paymentProfileId
    * def credit_card_length = paymentProfile.paymentProfilePayload.method.creditCard.creditCardNumber.length;

    Given path '/payment-profiles/' + id + '/token'
    When method get
    Then status 200

####################################
# Assert
####################################
    * match response == '#notnull'

@e2e
Feature: Payment-Platform API Tests - Payment Profile Controller

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }
    * def customerId = uuid()
    * def guestSessionId = uuid()
    * configure headers = { odmd-aakey: '#(xxxApiKey)' }

  Scenario: Stitch Payment Profile
  ####################################
    # Arrange
  ####################################

    # Create a Payment Profile Request
    * def paymentProfile = call read('classpath:helpers/createPaymentProfile.feature')  { requestId: #(requestId), customerId: #(guestSessionId) }

    ####################################
    # Act
    ####################################
    Given path '/payment-profiles/stitch'
    And header odmd-eee-id = customerId
    And request { guestSessionCustomerId: #(guestSessionId) }
    When method post
    Then status 200

 #  Get stitched profile
    Given path '/payment-profiles/'
    And header odmd-eee-id = customerId
    When method get
    Then status 200

    ####################################
    # Assert customer payment profile has data
   ###################################

    * match response[0].customerId != guestSessionId
    * match response[0].customerId == customerId
    * match response[0].reusable == paymentProfile.response.reusable
    * match response[0].default == paymentProfile.response.default
    * match response[0].paymentMethod.type == paymentProfile.response.paymentMethod.type
    * match response[0].paymentMethod.savedCreditCard.paymentProfileId == paymentProfile.response.paymentMethod.savedCreditCard.paymentProfileId
    * match response[0].paymentMethod.savedCreditCard.lastFourDigits == paymentProfile.response.paymentMethod.savedCreditCard.lastFourDigits
    * match response[0].paymentMethod.savedCreditCard.expirationMonth == paymentProfile.response.paymentMethod.savedCreditCard.expirationMonth
    * match response[0].paymentMethod.savedCreditCard.expirationYear == paymentProfile.response.paymentMethod.savedCreditCard.expirationYear
    * match response[0].paymentMethod.savedCreditCard.email == paymentProfile.response.paymentMethod.savedCreditCard.email
    * match response[0].metadata.lzDataPaymentProfileId == paymentProfile.response.metadata.lzDataPaymentProfileId

  ####################################
    # Assert that the guest profile no longer has a payment profile attached
   ###################################
    Given path '/payment-profiles/'
    And header odmd-eee-id = guestSessionId
    When method get
    Then status 200

    * match response ==  []
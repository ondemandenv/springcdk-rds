@e2e
Feature: Payment-Platform API Tests - Payment Profile Controller

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }

  Scenario: Update Payment Profile - Credit Card


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
    * def new_uuid = uuid()

    # Set Payment Profile Request
    * def updatePaymentProfilePayload = read('classpath:payloads/updatePaymentProfile.json')

    ####################################
    # Act
    ####################################

    Given path '/payment-profiles/' + id
    And request updatePaymentProfilePayload
    When method put
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response.id == id
    * match response.paymentMethod.savedCreditCard.expirationYear == updatePaymentProfilePayload.creditCard.expirationYear
    * match response.paymentMethod.savedCreditCard.expirationMonth == updatePaymentProfilePayload.creditCard.expirationMonth

  Scenario: Update Payment Profile While Authorize payment - Saved Credit Card
    * string customerId = uuid()
    * string originalEmail = "original-email@ondemandenv.dev"
    * string updatedEmail = "updated-email@ondemandenv.dev"

    ####################################
    # Arrange
    ####################################

    # Create Payment Profile with original email
    * def paymentProfile = call read('classpath:helpers/createPaymentProfile.feature')  { requestId: #(requestId), email: #(originalEmail), customerId: #(customerId) }

    ####################################
    # Act
    ####################################

    # Authorize payment with created profile, passing updated email
    * def paymentProfileId = paymentProfile.response.paymentMethod.savedCreditCard.paymentProfileId
    * def email = updatedEmail
    * def paymentMethod = read('classpath:payloads/savedCardPaymentMethod.json')
    * def authorizePaymentResponse = call read('classpath:helpers/authorize.feature')  { requestId: #(requestId), customerId: #(customerId), paymentMethod: #(paymentMethod) }

    ####################################
    # Assert
    ####################################

    # Get profile and verify the updated email is returned
    Given path '/payment-profiles/' + paymentProfileId
    And header odmd-eee-id = customerId
    And header odmd-aakey = xxxApiKey
    When method get
    Then status 200

    * match response.paymentMethod.savedCreditCard.billingInformation.email == updatedEmail

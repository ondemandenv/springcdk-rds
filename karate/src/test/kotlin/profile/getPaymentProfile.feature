@e2e
Feature: Payment-Platform API Tests - Payment Profile Controller

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }

  Scenario: Get Payment Profile - Credit Card
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

    Given path '/payment-profiles/' + id
    When method get
    Then status 200

####################################
# Assert
####################################
    * match response.id == id
    * match response.customerId == customerId
    * match response.paymentMethod.savedCreditCard.expirationMonth == paymentProfile.paymentProfilePayload.method.creditCard.expMonth
    * match response.paymentMethod.savedCreditCard.expirationYear == paymentProfile.paymentProfilePayload.method.creditCard.expYear
    * match response.paymentMethod.savedCreditCard.lastFourDigits == paymentProfile.paymentProfilePayload.method.creditCard.creditCardNumber.substring(credit_card_length -4, credit_card_length)
    * match response.paymentMethod.savedCreditCard.type == "VISA"
    * match response.reusable == paymentProfile.paymentProfilePayload.reusable

  Scenario: Get Active Payment Profiles for customer
    * string customerId = uuid()
    * configure headers = { odmd-eee-id: #(customerId), odmd-aakey: '#(xxxApiKey)' }
    * def validate1 = true
    * def validate2 = false

    ####################################
    # Arrange
    ####################################

    # Create two Payment Profiles
    * def paymentProfile1 = call read('classpath:helpers/createPaymentProfile.feature')  { requestId: #(requestId), validate: #(validate1) }
    * def paymentProfile2 = call read('classpath:helpers/createPaymentProfile.feature')  { requestId: #(requestId), validate: #(validate2) }

    ####################################
    # Act
    ####################################

    # Get Payment Profiles for customer
    Given path '/payment-profiles'
    When method get
    Then status 200
    * def actualPaymentProfiles = response.length

    ####################################
    # Assert
    ####################################
    * match actualPaymentProfiles == 2



    ####################################
    # Arrange
    ####################################

    # Inactivate second Payment Profile
    * def deletePaymentProfileId = paymentProfile2.response.id
    * def deletedPaymentProfile = call read('classpath:helpers/deletePaymentProfile.feature')  { customerId: #(customerId), profileId: #(deletePaymentProfileId) }

    ####################################
    # Act
    ####################################

    # Get Payment Profiles for customer
    Given path '/payment-profiles'
    When method get
    Then status 200
    * def actualActivePaymentProfiles = response.length

    ####################################
    # Assert
    ####################################
    * match actualActivePaymentProfiles == 1




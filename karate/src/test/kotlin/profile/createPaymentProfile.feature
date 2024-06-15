@e2e
Feature: Payment-Platform API Tests - Payment Profile Controller

  Background:
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }

  Scenario Outline: Create Payment Profile - Credit Card

    * string customerId = uuid()

    # Set Payment Profile Request
    * def paymentProfile = call read('classpath:helpers/createPaymentProfile.feature')  { customerId : #(customerId), gateway: <gateway> }

    ####################################
    # Assert
    ####################################
    * match paymentProfile.response.paymentMethod.savedCreditCard.paymentProfileId == "#notnull"
    * match paymentProfile.response.customerId == customerId

    # Note: for this to work locally you must activate the local Spring profile
    #       which has necessary configs for test gateway environments
    Examples:
    |gateway|
    |STRIPE |
    |WORLDPAY|

  Scenario: Invalid credit card fails with 402 (WorldPay)
    * string customerId = uuid()

    # Set Payment Profile Request
    * def paymentProfile = call read('classpath:helpers/createPaymentProfile.feature')  { status: 402, cardNumber: '4100282040123000', customerId: #(customerId), gateway: 'WORLDPAY' }

  Scenario: Invalid credit card fails with 402 (Stripe)
    * string customerId = uuid()

    # Set Payment Profile Request
    * def paymentProfile = call read('classpath:helpers/createPaymentProfile.feature')  { status: 402, cardNumber: 'InvalidStripeCreditCard', customerId: #(customerId), gateway: 'STRIPE' }

  Scenario: Create and validate credit card using WorldPay
    * string customerId = uuid()

    # Set Payment Profile Request
    * def paymentProfile = call read('classpath:helpers/createPaymentProfile.feature')  { customerId : #(customerId), gateway: 'WORLDPAY', validate: true }

    ####################################
    # Assert
    ####################################
    * match paymentProfile.response.paymentMethod.savedCreditCard.paymentProfileId == "#notnull"
    * match paymentProfile.response.customerId == customerId


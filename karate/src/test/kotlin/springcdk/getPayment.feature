@e2e
Feature: Payment-Platform API Tests - Payments Controller - get Payment

  Background:
    * url urlBase
    * def uuid = function(){ return java.util.UUID.randomUUID() + '' }
    * def customerId = uuid()
    * def result =  call read('classpath:helpers/authorize.feature') { customerId: #(customerId) }
    * def paymentId = result.response.springcdk[0].paymentId
    * def targetKey = result.response.targetKey
    * def targetType = result.response.targetType
    * configure headers = { odmd-eee-id: '#(customerId)', odmd-aakey: '#(xxxApiKey)' }

  ####################################
  # Get Payment information By PaymentId
  ####################################


  Scenario: Get Payment Information By PaymentId
    ####################################
    # Arrange
    ####################################
    Given path '/springcdk/' + paymentId
    When method get
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response.paymentId == paymentId

  Scenario: PaymentId Not Specified - Bad Request
    ####################################
    # Arrange
    ####################################
    Given path '/springcdk/'
    When method get
    Then status 400

  Scenario: Invalid PaymentId - Not Found
    ####################################
    # Arrange
    ####################################
    * def paymentId = 'test-1234'
    Given path '/springcdk/' + paymentId
    When method get
    Then status 403

  Scenario: EntityId Not Specified - Bad Request
    ####################################
    # Arrange
    ####################################
    * configure headers = { odmd-eee-id: '', odmd-aakey: '#(xxxApiKey)' }
    Given path '/springcdk/' + paymentId
    When method get
    Then status 403


  ####################################
  # Get Payment List
  ####################################


  Scenario: Get List springcdk related to a target
    ####################################
    # Arrange
    ####################################
    Given path '/springcdk'
    And param targetType = targetType
    And param targetKey = targetKey
    When method get
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response[0].targetType == targetType
    * match response[0].targetKey == targetKey

  Scenario: TargetType Not Specified - Bad Request
    ####################################
    # Arrange
    ####################################
    Given path '/springcdk'
    And param targetKey = targetKey
    When method get
    Then status 400

  Scenario: TargetKey Not Specified - Bad Request
    ####################################
    # Arrange
    ####################################
    Given path '/springcdk'
    And param targetType = targetType
    When method get
    Then status 400

  Scenario: EntityId Not Specified - Bad Request
    ####################################
    # Arrange
    ####################################
    * configure headers = { odmd-eee-id: '', odmd-aakey: '#(xxxApiKey)' }
    Given path '/springcdk'
    And param targetType = targetType
    And param targetKey = targetKey
    When method get
    Then status 403

  Scenario: Invalid targetType or targetKey - Get Empty List springcdk
    ####################################
    # Arrange
    ####################################
    Given path '/springcdk'
    And param targetType = targetType + '_test'
    And param targetKey = targetKey + '_test'
    When method get
    Then status 200

    ####################################
    # Assert
    ####################################
    * match response == []


  ########################################
  # Get Payments information By CustomerId
  #######################################


  Scenario: Get List of Payments by CustomerId
  ####################################
    # Arrange
  ####################################
    * def expectedPageNumber = 0
    * def expectedPageSize = 3
    * def expectedPaymentCount = result.response.springcdk.length

    Given path '/springcdk/search'
    And param customerId = customerId
    And param page = expectedPageNumber
    And param size = expectedPageSize
    When method get
    Then status 200

    ####################################
    # Assert
    ####################################
    #* match actualPaymentCount == result.response.springcdk.length
    * match expectedPaymentCount == response.content.length
    * match expectedPageSize == response.size
    * match expectedPageNumber == response.number


  Scenario: CustomerId Not Specified - Bad Request
  ####################################
    # Arrange
  ####################################
    Given path '/springcdk/search'
    When method get
    Then status 400
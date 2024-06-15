@ignore
Feature: Helper to Delete Payment Profile

  Background:
    * url urlBase

  Scenario: Delete Payment Profile by profileId

    ####################################
      # Arrange
    ####################################

    # Set Payment Profile Request
    * def headers = { odmd-eee-id: #(customerId), odmd-aakey: '#(xxxApiKey)' }

   ####################################
    # Act
    ####################################

    Given path '/payment-profiles/' + profileId
    And headers headers
    When method delete
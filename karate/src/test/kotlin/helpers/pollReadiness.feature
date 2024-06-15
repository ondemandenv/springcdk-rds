@ignore
Feature: Poll readiness endpoint to make sure app is running
  Background:
    * configure retry = { count: 5, interval: 60000 }
    * configure headers = { odmd-aakey: '#(xxxApiKey)' }
  Scenario:
    Given url urlBase
    And path '/actuator/health'
    And retry until responseStatus == 200
    When method get

@ignore
Feature: Reusable function to capture info about the date
  ################################################################################
  # Helper file to get current date, Month and year.
  #
  # Input Parameters:
  #None
  # Returns (i.e. variables that are available to the calling feature):
  # today =  (string) returns data in the format of 'yyyy-MM-dd'
  # currentMonth = (string) returns current month from today's date as integer value of current month like Jan - 1 , Feb - 2
  # currentYear = (string) returns current year of the today's date, like 2022, 2023
  ################################################################################
  # Example:
  # * def dateResponse = call read('classpath:helpers/getDate.feature')
  # * def depositDate = dateResponse.today

  Background: Setup
    * def pattern = 'yyyy-MM-dd'
    * def getDate =
      """
      function() {
        var SimpleDateFormat = Java.type('java.text.SimpleDateFormat');
        var sdf = new SimpleDateFormat(pattern);

        var TimeZone = Java.type('java.util.TimeZone');
        sdf.setTimeZone(TimeZone.getTimeZone("PST"));

        var depositDate = new java.util.Date();
        return sdf.format(depositDate);
      }
      """
    * def monthPattern = 'MM'
    * def getCurrentMonth =
      """
      function() {
        var SimpleDateFormat = Java.type('java.text.SimpleDateFormat');
        var sdf = new SimpleDateFormat(monthPattern);

        var TimeZone = Java.type('java.util.TimeZone');
        sdf.setTimeZone(TimeZone.getTimeZone("PST"));

        var depositDate = new java.util.Date();
        return sdf.format(depositDate);
      }
      """
    * def yearPattern = 'yyyy'
    * def getCurrentYear =
      """
      function() {
        var SimpleDateFormat = Java.type('java.text.SimpleDateFormat');
        var sdf = new SimpleDateFormat(yearPattern);

        var TimeZone = Java.type('java.util.TimeZone');
        sdf.setTimeZone(TimeZone.getTimeZone("PST"));

        var depositDate = new java.util.Date();
        return sdf.format(depositDate);
      }
      """

  Scenario: Capture today date
    * def today = getDate()
    * def currentMonth = getCurrentMonth()
    * def currentYear = getCurrentYear()
# Scenario files for Imprisonment Status calculation test
`uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusCalculationTest#should calculate Imprisonment Status`

## Background

Jira ticket [IS-64](https://dsdmoj.atlassian.net/browse/IS-64) implements code and processes to notify the development
team of any mismatched Imprisonment Status calculation. Mismatched calculations are automatically created as Jira tickets
for the development team to investigate and implement a fix.

The test class `uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusCalculationTest#should calculate Imprisonment Status`
contains a test that calculates and asserts the Imprisonment Status from data in a suite of scenario files.

This folder contains the scenario files.

## Developer process

For each mismatched calculation that results in a Jira ticket the developer should:

* Create a new json file in this folder, with the Jira reference as the filename
* Populate the json file as described below
* Add the Jira reference to the `@CsvSource` annotation on the test
* Run the tests. The new test should be a failing test
* Refactor the implementation to make the tests pass

### Production data / PII

Be aware that each scenario is sourced from production data. Whilst the data you will be populating scenario files with
is production data pertaining to prisoner's offences and sentences, it should not contain any PII or similar data
including prison number / PRN.

**It is the responsibility of the development team / PR process to make sure that no PII is committed in these files.**

### Populating the json file

Each json file represents a single scenario and is referenced to the Jira ticket via it's filename.

Each json file must have the following properties:
```
{
  "jiraReference": <String>
  "receptionDate": <String>
  "expectedStatus": <String>
  "rasChargeData": <PersonCharges>
  "rasSentenceTypeDetails": <Array[SentenceTypeDetails]>
  "offenceSeverityRatings": <Array[OffenceSeverityRating]>        
}
```

* `jiraReference`  
  The Jira ticket number
* `receptionDate`  
  The receptionDate property for the prisoner from prisoner-search-api
* `expectedStatus`  
  The expected Imprisonment Status
* `rasChargeData`  
  The prisoner's charge data from RaS. The Jira ticket will contain a DB record from which you can get this data
* `rasSentenceTypeDetails`  
  As part of processing the scenario, the calculation implementation will lookup the sentence type for each sentence in 
  the `rasChargeData`.  
  Find the `sentenceTypeUuid` property for each sentence (each charge will have 0 or 1 sentences), then call the RaS API
  to lookup the sentence type data.
  Paste the response data into the `rasSentenceTypeDetails` property.    
  There may be duplicate sentence types - a prisoner may have been sentenced with multiple counts of the same sentence. 
  You only need one of each type in the `rasSentenceTypeDetails` property.
* `offenceSeverityRatings`  
  As part of processing the scenario, the calculation implementation will lookup the offence severity rating for each
  offence in the `rasChargeData`.  
  Find the `offenceCode` properties (one for each charge), then call the `manage-offences-api` to get the severity data.   
  Paste the response data into the `offenceSeverityRatings` property.    
  There may be duplicate offence codes - a prisoner may have been charged with multiple counts of the same offence. You
  only need one of each type in the `offenceSeverityRatings` property.

### Examples
The files `IS-00-example.json` and `IS-01-example.json` are worked examples.

* `IS-00-example.json` is a scenario of a prisoner serving several concurrent sentences for several offences.
* `IS-01-example.json` is a scenario of an un-sentenced prisoner on remand for a single offence. 

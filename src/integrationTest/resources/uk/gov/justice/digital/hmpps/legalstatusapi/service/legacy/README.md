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
  "previousPeriodConcludedAt": <String> (optional)
  "expectedStatus": <String>
  "rasData": <
    "personCharges": <PersonCharges>
    "sentenceTypeDetails": Map<UUID, SentenceTypeDetails>
  >
  "offenceSeverities": <Map<Sring, Int>>        
}
```

* `jiraReference`  
  The Jira ticket number
* `receptionDate`  
  The receptionDate property for the prisoner from prisoner-search-api. It is not used in the calculation.
* `previousPeriodConcludedAt` (optional)  
  When the prisoner's previous period of custody concluded, from the Person Location API, as an ISO instant (for example
  `2025-01-20T09:00:00Z`). The test gives the prisoner a period of custody with this previous period. The calculation
  selects the start of the date range for the RaS charges from it. Copy the contents of the
  `previous_period_concluded_at` database field into this property. If the field is null, or the mismatch was recorded
  before IS-162, leave this property out. Then there is no previous period, and the RaS API is called with no
  `fromDate`.
* `expectedStatus`  
  The expected Imprisonment Status
* `rasData`  
  The data from the RaS API as looked up by the calculation implementation when the scenario was first run.  
  The Jira ticket will contain a DB record from which you can get this data. Copy the contents of the `ras_data`
  database field into this property.
* `offenceSeverities`  
  The offence severity data as looked up by the calculation implementation when the scenario was first run.  
  The Jira ticket will contain a DB record from which you can get this data. Copy the contents of the `offence_severities`
  database field into this property.

### Examples
The files `IS-00-example.json` and `IS-01-example.json` are worked examples.

* `IS-00-example.json` is a scenario of a prisoner serving several concurrent sentences for several offences.
* `IS-01-example.json` is a scenario of an un-sentenced prisoner on remand for a single offence. 

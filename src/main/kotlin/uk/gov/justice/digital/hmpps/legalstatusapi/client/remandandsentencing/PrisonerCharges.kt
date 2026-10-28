package uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing

import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge

/**
 * A prisoner's charges from the RaS `GET /person/{prisonNumber}/charges` endpoint.
 *
 * @property rawJson The response body exactly as RaS returned it.
 */
data class PrisonerCharges(
  val charges: List<PersonCharge>,
  val rawJson: String,
)

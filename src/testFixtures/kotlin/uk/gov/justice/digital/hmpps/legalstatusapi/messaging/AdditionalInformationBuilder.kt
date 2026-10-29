package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation

fun aValidImprisonmentStatusChangedAdditionalInformation(
  bookingId: Long = 1240469,
): ImprisonmentStatusChangedAdditionalInformation = ImprisonmentStatusChangedAdditionalInformation(
  bookingId = bookingId,
)

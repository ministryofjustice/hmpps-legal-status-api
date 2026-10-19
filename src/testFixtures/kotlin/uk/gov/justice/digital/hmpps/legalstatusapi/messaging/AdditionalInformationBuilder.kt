package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation

fun aValidImprisonmentStatusChangedAdditionalInformation(
  bookingId: String = "1242230",
): ImprisonmentStatusChangedAdditionalInformation = ImprisonmentStatusChangedAdditionalInformation(
  bookingId = bookingId,
)

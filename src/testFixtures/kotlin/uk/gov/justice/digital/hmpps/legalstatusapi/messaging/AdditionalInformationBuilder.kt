package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ChargeAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusComputableAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.SentenceAdditionalInformation
import java.util.UUID

fun aValidImprisonmentStatusChangedAdditionalInformation(
  bookingId: Long = 1240469,
): ImprisonmentStatusChangedAdditionalInformation = ImprisonmentStatusChangedAdditionalInformation(
  bookingId = bookingId,
)

fun aValidSentenceAdditionalInformation(
  sentenceId: UUID = aValidReference(),
  courtChargeId: UUID = aValidReference(),
  courtCaseId: UUID = aValidReference(),
  courtAppearanceId: UUID = aValidReference(),
  source: MessageSource = MessageSource.NOMIS,
  isBreach: Boolean = false,
): SentenceAdditionalInformation = SentenceAdditionalInformation(
  sentenceId = sentenceId,
  courtChargeId = courtChargeId,
  courtCaseId = courtCaseId,
  courtAppearanceId = courtAppearanceId,
  source = source,
  isBreach = isBreach,
)

fun aValidChargeAdditionalInformation(
  courtChargeId: UUID = aValidReference(),
  courtCaseId: UUID = aValidReference(),
  source: MessageSource = MessageSource.NOMIS,
  courtAppearanceId: UUID = aValidReference(),
  isOnFutureAppearance: Boolean = false,
  isBreach: Boolean = false,
): ChargeAdditionalInformation = ChargeAdditionalInformation(
  courtChargeId = courtChargeId,
  courtCaseId = courtCaseId,
  source = source,
  courtAppearanceId = courtAppearanceId,
  isOnFutureAppearance = isOnFutureAppearance,
  isBreach = isBreach,
)

fun aValidImprisonmentStatusComputableAdditionalInformation(): ImprisonmentStatusComputableAdditionalInformation = ImprisonmentStatusComputableAdditionalInformation()

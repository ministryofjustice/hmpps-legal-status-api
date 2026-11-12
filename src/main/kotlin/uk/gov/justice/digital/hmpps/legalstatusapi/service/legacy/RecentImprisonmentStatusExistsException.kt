package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.Instant

class RecentImprisonmentStatusExistsException(val prisonNumber: String, val lastCalculatedAt: Instant) : RuntimeException("Recent ImprisonmentStatus for $prisonNumber already exists")

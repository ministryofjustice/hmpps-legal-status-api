package uk.gov.justice.digital.hmpps.legalstatusapi.config

import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.DOMAIN_EVENTS_QUEUE_ID
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.SqsMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

class MessagingHelper(
  val hmppsQueueService: HmppsQueueService,
  val objectMapper: ObjectMapper,
) {
  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic")
      ?: throw MissingTopicException("HmppsTopic hmppseventtopic not found")
  }

  val domainEventsQueue by lazy {
    hmppsQueueService.findByQueueId(DOMAIN_EVENTS_QUEUE_ID)
      ?: throw MissingQueueException("HmppsQueue $DOMAIN_EVENTS_QUEUE_ID not found")
  }

  val domainEventsQueueClient by lazy { domainEventsQueue.sqsClient }

  val domainEventsQueueDlqClient by lazy { domainEventsQueue.sqsDlqClient }

  fun purgeDomainEventsQueues() {
    domainEventsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueue.queueUrl).build()).get()
    domainEventsQueue.sqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueue.dlqUrl).build()).get()
  }

  fun sendDomainEvent(
    message: SqsMessage,
    queueUrl: String = domainEventsQueue.queueUrl,
  ): SendMessageResponse = domainEventsQueueClient.sendMessage(
    SendMessageRequest.builder()
      .queueUrl(queueUrl)
      .messageBody(
        objectMapper.writeValueAsString(message),
      ).build(),
  ).get()

  fun messagesOnDomainEventsQueue(): Int = domainEventsQueueClient.countAllMessagesOnQueue(domainEventsQueue.queueUrl).get()

  fun messagesOnDomainEventsDlq(): Int = domainEventsQueueDlqClient!!.countAllMessagesOnQueue(domainEventsQueue.dlqUrl!!).get()
}

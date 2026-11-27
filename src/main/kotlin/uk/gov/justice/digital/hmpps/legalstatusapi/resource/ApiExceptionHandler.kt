package uk.gov.justice.digital.hmpps.legalstatusapi.resource

import io.sentry.Sentry
import jakarta.validation.ValidationException
import mu.KotlinLogging
import org.springframework.context.MessageSourceResolvable
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.legalstatusapi.resource.model.ErrorResponse

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST.value(),
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(
    IllegalArgumentException::class,
    IllegalStateException::class,
    HttpMessageNotReadableException::class,
  )
  fun handleGenericBadRequests(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST.value(),
        userMessage = "Invalid request",
        developerMessage = e.devMessage(),
      ),
    ).also { log.error(e.message, e) }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST.value(),
        userMessage = "Method argument type mismatch, expecting ${e.requiredType}",
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> = e.allErrors.mapErrors()

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> = e.allErrors.mapErrors()

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(ErrorResponse(status = FORBIDDEN.value()))

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNotFoundException(): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(ErrorResponse(status = NOT_FOUND.value()))

  @ExceptionHandler(DataIntegrityViolationException::class, DataAccessException::class)
  fun handleConflictException(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(CONFLICT)
    .body(
      ErrorResponse(
        status = CONFLICT.value(),
        userMessage = "Data integrity conflict",
        developerMessage = e.devMessage(),
      ),
    ).also { log.error(e.message, e) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR.value(),
        userMessage = "Unexpected error",
        developerMessage = e.devMessage(),
      ),
    ).also { log.error(e.message, e) }
}

private fun Exception.devMessage(): String {
  val sentryId = Sentry.captureException(this)
  return "${this::class.simpleName}: $sentryId"
}

private fun List<MessageSourceResolvable>.mapErrors() = map { it.defaultMessage!! }.distinct().sorted().let {
  val validationFailure = "Validation failure"
  val message = if (it.size > 1) {
    """
    |${validationFailure}s: 
    |${it.joinToString(System.lineSeparator())}
    |
    """.trimMargin()
  } else {
    "$validationFailure: ${it.joinToString(System.lineSeparator())}"
  }
  ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST.value(),
        userMessage = message,
        developerMessage = "400 BAD_REQUEST $message",
      ),
    )
}

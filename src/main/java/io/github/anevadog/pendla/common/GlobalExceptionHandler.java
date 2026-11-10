package io.github.anevadog.pendla.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.github.anevadog.pendla.application.DuplicateJobApplicationException;
import io.github.anevadog.pendla.application.InvalidStatusTransitionException;
import io.github.anevadog.pendla.application.JobApplicationNotFoundException;
import io.github.anevadog.pendla.jobsearch.JobTechTimeoutException;
import io.github.anevadog.pendla.jobsearch.JobTechUnavailableException;
import io.github.anevadog.pendla.jobsearch.UnsupportedCityException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Single place where exceptions become HTTP error responses (ProblemDetail, RFC 9457).
 *
 * Extending ResponseEntityExceptionHandler gives ProblemDetail responses for Spring MVC's own errors
 * (malformed JSON, wrong types, unsupported method...). We only add our exceptions and customize validation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(JobApplicationNotFoundException.class)
	public ProblemDetail handleNotFound(JobApplicationNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(DuplicateJobApplicationException.class)
	public ProblemDetail handleDuplicate(DuplicateJobApplicationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	/** The request is well formed but conflicts with the current status of the application. */
	@ExceptionHandler(InvalidStatusTransitionException.class)
	public ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(UnsupportedCityException.class)
	public ProblemDetail handleUnsupportedCity(UnsupportedCityException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/** JobTech failed or answered something we cannot read: the problem is upstream, not in the request. */
	@ExceptionHandler(JobTechUnavailableException.class)
	public ProblemDetail handleJobTechUnavailable(JobTechUnavailableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(JobTechTimeoutException.class)
	public ProblemDetail handleJobTechTimeout(JobTechTimeoutException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
	}

	/**
	 * The JSON body could not be read. If the cause is an unknown enum value (e.g. "status": "FOO"),
	 * the answer lists the valid values; any other case keeps Spring's default 400.
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (ex.getCause() instanceof InvalidFormatException cause && cause.getTargetType().isEnum()) {
			// The path says where the bad value was: its last step is the JSON field name
			String field = cause.getPath().isEmpty() ? "value" : cause.getPath().getLast().getPropertyName();
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
					invalidEnumDetail(field, cause.getValue(), cause.getTargetType()));
			return handleExceptionInternal(ex, problem, headers, status, request);
		}
		return super.handleHttpMessageNotReadable(ex, headers, status, request);
	}

	/** Same idea for a query parameter or path variable that is an enum (e.g. ?status=FOO). */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Class<?> requiredType = ex.getRequiredType();
		if (requiredType != null && requiredType.isEnum()) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
					invalidEnumDetail(ex.getPropertyName(), ex.getValue(), requiredType));
			return handleExceptionInternal(ex, problem, headers, status, request);
		}
		return super.handleTypeMismatch(ex, headers, status, request);
	}

	/** Example: Invalid value 'FOO' for 'status'. Allowed values: SAVED, SENT, INTERVIEW, REJECTED, OFFER */
	private static String invalidEnumDetail(String name, Object value, Class<?> enumType) {
		List<String> allowed = new ArrayList<>();
		for (Object constant : enumType.getEnumConstants()) {
			allowed.add(constant.toString());
		}
		return "Invalid value '" + value + "' for '" + name + "'. Allowed values: " + String.join(", ", allowed);
	}

	/** Constraints on @RequestParam failed (e.g. blank keyword): 400 with one message per parameter. */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			String parameter = result.getMethodParameter().getParameterName();
			for (MessageSourceResolvable error : result.getResolvableErrors()) {
				errors.putIfAbsent(parameter, error.getDefaultMessage());
			}
		}

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	/** @Valid failed on a request body: return 400 with one message per invalid field. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request data");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

}

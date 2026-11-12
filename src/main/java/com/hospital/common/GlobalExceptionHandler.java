package com.hospital.common;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends DataFetcherExceptionResolverAdapter {

    // ── REST ────────────────────────────────────────────────────────────────

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handle(DomainException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getCode().name());
        body.put("message", ex.getMessage());
        if (ex.getExtras() != null) {
            body.put("suggested_alternatives", ex.getExtras());
        }
        return ResponseEntity.status(ex.getCode().httpStatus).body(body);
    }

    // ── GraphQL ──────────────────────────────────────────────────────────────

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (!(ex instanceof DomainException de)) return null;

        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("code", de.getCode().name());
        if (de.getExtras() != null) {
            extensions.put("suggested_alternatives", de.getExtras());
        }

        return GraphqlErrorBuilder.newError(env)
                .message(de.getMessage())
                .extensions(extensions)
                .errorType(toErrorType(de.getCode()))
                .build();
    }

    private static ErrorType toErrorType(ErrorCode code) {
        return switch (code.httpStatus) {
            case 401 -> ErrorType.UNAUTHORIZED;
            case 403 -> ErrorType.FORBIDDEN;
            case 404 -> ErrorType.NOT_FOUND;
            default  -> ErrorType.BAD_REQUEST;
        };
    }
}

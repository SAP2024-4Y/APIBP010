package com.hospital.common;

public enum ErrorCode {
    APPOINTMENT_CONFLICT(409),
    SLOT_UNAVAILABLE(409),
    PATIENT_NOT_FOUND(404),
    DOCTOR_NOT_FOUND(404),
    HOSPITAL_NOT_FOUND(404),
    INVALID_DATE_RANGE(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    RATE_LIMITED(429),
    OUTSIDE_WORKING_HOURS(422),
    MINIMUM_BUFFER_VIOLATION(422);

    public final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }
}

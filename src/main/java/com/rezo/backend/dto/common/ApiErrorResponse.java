package com.rezo.backend.dto.common;

import java.time.OffsetDateTime;

public class ApiErrorResponse {

    private final boolean success;
    private final Object data;
    private final ApiError error;

    public ApiErrorResponse(boolean success, Object data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(
                false,
                null,
                new ApiError(code, message, status, path, OffsetDateTime.now().toString())
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public static class ApiError {
        private final String code;
        private final String message;
        private final int status;
        private final String path;
        private final String timestamp;

        public ApiError(String code, String message, int status, String path, String timestamp) {
            this.code = code;
            this.message = message;
            this.status = status;
            this.path = path;
            this.timestamp = timestamp;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }

        public String getPath() {
            return path;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}
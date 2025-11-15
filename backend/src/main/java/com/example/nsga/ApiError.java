package com.example.nsga;


import java.time.Instant;
import java.util.List;

public class ApiError {
    private Instant timestamp = Instant.now();
    private int status;              // 400, 404, 500...
    private String error;            // "Bad Request", "Not Found"...
    private String code;             // mã nội bộ, ví dụ "USER_NOT_FOUND"
    private String message;          // thông điệp cho client
    private String path;             // request path
    private List<FieldViolation> errors; // lỗi chi tiết (validate)

    public static class FieldViolation {
        private String field;
        private String reason;
        public FieldViolation(String field, String reason) {
            this.field = field; this.reason = reason;
        }
        public String getField() { return field; }
        public String getReason() { return reason; }
    }

    // getters/setters
    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public List<FieldViolation> getErrors() { return errors; }
    public void setErrors(List<FieldViolation> errors) { this.errors = errors; }
}

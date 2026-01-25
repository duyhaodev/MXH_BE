package CloneThreads.Threads.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    INVALID_KEY(1001, "Invalid message key",  HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User already existed", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "You do not have permission", HttpStatus.FORBIDDEN),
    PASSWORD_INVALID(1005, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1006, "Invalid email address", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_BLANK(1007, "Username must not be blank", HttpStatus.BAD_REQUEST),
    FULLNAME_NOT_BLANK(1008, "Full name must not be blank", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_BLANK(1009, "Email must not be blank", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1010, "User not found", HttpStatus.NOT_FOUND),
    TOKEN_INVALID(1011, "Invalid token", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_FOUND(1012, "Conversation not found", HttpStatus.NOT_FOUND),
    CANNOT_FOLLOW_SELF(1013, "CANNOT_FOLLOW_SELF", HttpStatus.BAD_REQUEST),
    ALREADY_FOLLOWING(1014, "ALREADY_FOLLOWING", HttpStatus.CONFLICT),
    NOT_FOLLOWING(1015, "NOT_FOLLOWING", HttpStatus.NOT_FOUND),
    INVALID_NOTIFICATION(1016,"Invalid notification", HttpStatus.BAD_REQUEST),
    NOTIFICATION_NOT_FOUND(1017,"Notification not found", HttpStatus.NOT_FOUND),
    INVALID_OTP_KEY(1018,"Invalid verification code", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1019,"Verification code expired", HttpStatus.BAD_REQUEST),
    MAX_OTP_ATTEMPTS(1020,"Max verification attempts reached. Please request a new code.", HttpStatus.TOO_MANY_REQUESTS),
    USER_NOT_ENABLED(1021, "Your account is not verified. Please verify your email.", HttpStatus.FORBIDDEN),
    WRONG_EMAIL_PASSWORD(1022, "Wrong email or password. Please try again.", HttpStatus.FORBIDDEN),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;
}

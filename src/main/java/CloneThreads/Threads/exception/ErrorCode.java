package CloneThreads.Threads.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    INVALID_KEY(1001, "Invalid message key",  HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User already existed", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(1003, "Incorrect email or password", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "You do not have permission", HttpStatus.FORBIDDEN),
    PASSWORD_INVALID(1005, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1006, "Invalid email address", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_BLANK(1007, "Username must not be blank", HttpStatus.BAD_REQUEST),
    FULLNAME_NOT_BLANK(1008, "Full name must not be blank", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_BLANK(1009, "Email must not be blank", HttpStatus.BAD_REQUEST),
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

package CloneThreads.Threads.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserCreationRequest {
    @NotBlank(message = "USERNAME_NOT_BLANK")
    String userName;

    @NotBlank(message = "NAME_NOT_BLANK")
    String name;

    @NotBlank(message = "EMAIL_NOT_BLANK")
    @Email(message = "EMAIL_INVALID")
    String email;
    String password;

    LocalDate birthDate;
}

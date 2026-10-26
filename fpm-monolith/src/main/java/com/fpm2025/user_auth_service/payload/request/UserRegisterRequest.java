package com.fpm2025.user_auth_service.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest  {
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email là bắt buộc")
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    // BR-AUTH-02: >= 8 ky tu, co chu hoa, chu so va ky tu dac biet
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "Mật khẩu phải có chữ hoa, chữ số và ký tự đặc biệt")
    private String password;

    @NotBlank(message = "Tên người dùng là bắt buộc")
    private String username;

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
    
    
}

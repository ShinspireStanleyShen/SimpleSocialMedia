package com.esun.socialmedia.service;

import com.esun.socialmedia.common.exception.BusinessException;
import com.esun.socialmedia.common.util.JwtUtils;
import com.esun.socialmedia.common.util.SecurityUtils;
import com.esun.socialmedia.domain.dto.request.LoginRequest;
import com.esun.socialmedia.domain.dto.request.RegisterRequest;
import com.esun.socialmedia.domain.dto.response.LoginResponse;
import com.esun.socialmedia.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SecurityUtils  securityUtils;
    private final JwtUtils       jwtUtils;

    public Long register(RegisterRequest req) {
        String salt     = securityUtils.generateSalt();
        String hashPwd  = securityUtils.hashPassword(req.getPassword(), salt);
        String safeName = securityUtils.sanitize(req.getUserName());
        String safeEmail = req.getEmail() != null ? securityUtils.sanitize(req.getEmail()) : null;

        Map<String, Object> out = userRepository.register(
                req.getPhone(), safeName, safeEmail, hashPwd, salt);

        int result = (Integer) out.get("result");
        if (result == 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "此手機號碼已被註冊");
        }
        if (result != 0) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "註冊失敗，請稍後再試");
        }
        return (Long) out.get("userId");
    }

    public LoginResponse login(LoginRequest req) {
        Map<String, Object> user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "手機號碼或密碼錯誤"));

        String storedHash = (String) user.get("password");
        String salt       = (String) user.get("salt");

        if (!securityUtils.verifyPassword(req.getPassword(), salt, storedHash)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "手機號碼或密碼錯誤");
        }

        Long   userId   = (Long)   user.get("userId");
        String phone    = (String) user.get("phone");
        String userName = (String) user.get("userName");
        String token    = jwtUtils.generateToken(userId, phone);

        return LoginResponse.builder()
                .userId(userId)
                .userName(userName)
                .phone(phone)
                .token(token)
                .build();
    }
}

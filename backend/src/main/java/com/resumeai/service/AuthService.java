package com.resumeai.service;

import com.resumeai.dto.AuthResponse;
import com.resumeai.dto.LoginRequest;
import com.resumeai.dto.RegisterRequest;
import com.resumeai.dto.ResetPasswordRequest;
import com.resumeai.entity.EmailVerificationToken;
import com.resumeai.entity.PasswordResetToken;
import com.resumeai.entity.RefreshToken;
import com.resumeai.entity.User;
import com.resumeai.exception.ApiException;
import com.resumeai.repository.EmailVerificationTokenRepository;
import com.resumeai.repository.PasswordResetTokenRepository;
import com.resumeai.repository.RefreshTokenRepository;
import com.resumeai.repository.UserRepository;
import com.resumeai.security.JwtService;
import com.resumeai.security.OpaqueTokenGenerator;
import com.resumeai.service.email.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private static final long EMAIL_VERIFICATION_TTL_MINUTES = 60 * 24;
    private static final long PASSWORD_RESET_TTL_MINUTES = 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final EmailSender emailSender;
    private final String frontendBaseUrl;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, RefreshTokenRepository refreshTokenRepository,
                        EmailVerificationTokenRepository emailVerificationTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        OpaqueTokenGenerator opaqueTokenGenerator, EmailSender emailSender,
                        @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
        this.emailSender = emailSender;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .emailVerified(false)
                .plan(User.Plan.FREE)
                .build();

        userRepository.save(user);
        sendVerificationEmail(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = jwtService.hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session expired. Please log in again."));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session expired. Please log in again.");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session expired. Please log in again."));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = jwtService.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        OpaqueTokenGenerator.Token token = opaqueTokenGenerator.generate(EMAIL_VERIFICATION_TTL_MINUTES);
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(token.hash())
                .expiresAt(token.expiresAt())
                .used(false)
                .build());

        String link = frontendBaseUrl + "/verify-email?token=" + token.rawValue();
        emailSender.send(user.getEmail(), "Verify your email",
                "Welcome to Resume Intelligence. Verify your email by opening this link: " + link
                + "\n\nThis link expires in 24 hours.");
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = jwtService.hash(rawToken);
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "This verification link is invalid or has expired."));

        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "This verification link is invalid or has expired.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "This verification link is invalid or has expired."));

        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        emailVerificationTokenRepository.save(token);
    }

    @Transactional
    public void requestPasswordReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            OpaqueTokenGenerator.Token token = opaqueTokenGenerator.generate(PASSWORD_RESET_TTL_MINUTES);
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(token.hash())
                    .expiresAt(token.expiresAt())
                    .used(false)
                    .build());

            String link = frontendBaseUrl + "/reset-password?token=" + token.rawValue();
            emailSender.send(user.getEmail(), "Reset your password",
                    "Reset your password by opening this link: " + link
                    + "\n\nThis link expires in 1 hour. If you did not request this, you can ignore this email.");
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = jwtService.hash(request.token());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "This reset link is invalid or has expired."));

        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "This reset link is invalid or has expired.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "This reset link is invalid or has expired."));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        JwtService.RawRefreshToken refresh = jwtService.generateRefreshToken();

        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refresh.hash())
                .expiresAt(refresh.expiresAt())
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);

        return new AuthResponse(accessToken, refresh.rawValue(), user.getEmail(), user.getFullName());
    }
}


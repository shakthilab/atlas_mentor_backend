package com.lab.atlasmentor.service;

import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.RefreshToken;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration:604800}")
    private long refreshExpirationSeconds;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    public record TokenPair(String accessToken, String refreshToken) {}

    /** Creates a new refresh token for the user and returns the raw (unhashed) value. */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();

        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(hash(rawToken));
        entity.setUser(user);
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationSeconds));
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    /**
     * Validates the incoming refresh token, revokes it, and issues a fresh pair.
     * If the token was already revoked (reuse attack), all tokens for that user are revoked.
     */
    @Transactional
    public TokenPair rotate(String rawToken, User user) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (stored.isRevoked()) {
            // Possible token theft — revoke everything for this user
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId());
            throw new BusinessException("Refresh token already used. All sessions have been invalidated for security.");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new BusinessException("Refresh token has expired. Please log in again.");
        }

        // Rotate: revoke old, issue new pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken = jwtService.generateToken(
                user.getEmail(), user.getId(),
                user.getRole().getName(), user.getBranchId());
        String newRawRefreshToken = createRefreshToken(user);

        return new TokenPair(newAccessToken, newRawRefreshToken);
    }

    /** Revokes a single refresh token (logout). */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /** Revokes all refresh tokens for a user (logout-all / security reset). */
    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("SHA-256 unavailable", e);
        }
    }
}

//package com.fpm2025.user_auth_service.util;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import com.fpm2025.user_auth_service.entity.UserEntity;
//
//import java.util.Date;
//import java.util.Map;
//
//import javax.crypto.SecretKey;
//
//@Component
//public class JwtUltils {
//    @Value("${key.token.jwt}")
//    private String strKeyToken;
//
//    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 giờ
//    private SecretKey getSigningKey() {
//        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(strKeyToken));
//    }
//
//    public String createToken(UserEntity user) {
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
//
//        return Jwts.builder()
//                .subject(user.getEmail()) // subject là email
//                .claim("id", user.getUserId())
//                .claim("name", user.getFullName())
//                .claim("avatar", user.getAvatar())
//                .claim("user_type", user.getUserType() != null ? user.getUserType() : "USER")
//                .issuedAt(now)
//                .expiration(expiryDate)
//                .signWith(getSigningKey())
//                .compact();
//    }
//
//    public String createToken(Map<String, Object> claims) {
//        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(strKeyToken));
//
//        return Jwts.builder()
//                .claims(claims) // không dùng setClaims nữa
//                .subject("Login JWT") // không dùng setSubject
//                .issuedAt(new Date())
//                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24h
//                .signWith(secretKey) // không cần truyền SignatureAlgorithm nữa
//                .compact();
//    }
//
//
////    TODO: Giải mã token
//    public String decryptToke(String token){
//        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(strKeyToken));
//        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
//    }
//    
//    public Claims decryptTokenClaims(String token) {
//        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(strKeyToken));
//        return Jwts.parser()
//                .verifyWith(secretKey)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload(); // trả về toàn bộ payload (claims)
//    }
//
//}

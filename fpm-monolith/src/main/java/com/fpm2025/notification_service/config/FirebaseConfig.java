package com.fpm2025.notification_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK initialization.
 *
 * Đọc service-account JSON từ:
 *   1. Classpath (src/main/resources/firebase-service-account.json)
 *   2. File system path (absolute hoặc relative)
 *
 * Khi firebase.enabled=false → skip initialization, FCM sẽ chạy simulation mode.
 * Khi firebase.enabled=true  → khởi tạo Firebase SDK thật, push notification qua FCM.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials-path:firebase-service-account.json}")
    private String credentialsPath;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!firebaseEnabled) {
            log.warn("═══════════════════════════════════════════════════════");
            log.warn("  Firebase DISABLED (firebase.enabled=false)");
            log.warn("  FCM push sẽ chạy ở SIMULATION mode.");
            log.warn("  Để bật: set FIREBASE_ENABLED=true + cung cấp credentials.");
            log.warn("═══════════════════════════════════════════════════════");
            return null;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = loadCredentials();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK initialized successfully.");
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.error("❌ Firebase initialization FAILED: {}. FCM will run in simulation mode.", e.getMessage());
            return null;
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        // 1. Try classpath first
        InputStream classpathStream = getClass().getClassLoader().getResourceAsStream(credentialsPath);
        if (classpathStream != null) {
            log.info("Loading Firebase credentials from classpath: {}", credentialsPath);
            return GoogleCredentials.fromStream(classpathStream);
        }

        // 2. Try file system
        log.info("Loading Firebase credentials from file system: {}", credentialsPath);
        return GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
    }
}

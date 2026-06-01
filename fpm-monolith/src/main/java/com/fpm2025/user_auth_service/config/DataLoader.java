package com.fpm2025.user_auth_service.config;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component
@Profile("!test")
public class DataLoader {
	@org.springframework.beans.factory.annotation.Value("${seeding.users.csv:classpath:config/user.csv}")
	private Resource usersCsv;
	
	@PostConstruct
	public void loadData() {
		try {
			if (usersCsv == null || !usersCsv.exists()) {
				System.err.println("[DataLoader] WARN: user.csv missing. Skipping data load.");
				return;
			}
			try (Reader reader = new InputStreamReader(usersCsv.getInputStream(), StandardCharsets.UTF_8)) {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT
						.withFirstRecordAsHeader()
						.parse(reader);
				for (CSVRecord record : records) {
					String username = record.get("username");
					String email = record.get("email");
					// Lưu vào database (cần Entity và Repository)
					System.out.println("Loaded user: " + username + ", " + email);
				}
			}
		} catch (Exception e) {
			System.err.println("[DataLoader] ERROR: Failed to load user.csv: " + e.getMessage());
		}
	}
}
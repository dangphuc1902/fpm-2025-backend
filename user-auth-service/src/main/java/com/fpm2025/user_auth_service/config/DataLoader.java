package com.fpm2025.user_auth_service.config;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component
public class DataLoader {
	@Value("classpath:D:\\WorkSpace\\Project\\FPM-2025\\config\\config\\src\\main\\resources\\config\\user.csv")
	private Resource usersCsv;
	
	@PostConstruct
	public void loadData() throws Exception {
		try (Reader reader = new InputStreamReader(usersCsv.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);
            for (CSVRecord record : records) {
                String username = record.get("username");
                String password = record.get("password");
                String email = record.get("email");
                // Lưu vào database (cần Entity và Repository)
                System.out.println("Loaded user: " + username + ", " + email);
            }
        }
	}
}
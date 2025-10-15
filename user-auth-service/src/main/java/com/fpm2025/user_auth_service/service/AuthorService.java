//package com.fpm2025.user_auth_service.service;
//
////import com.WorkStudySync.entity.UserEntity;
////import com.WorkStudySync.entity.enums.UserTypeEnum;
////import com.WorkStudySync.exception.UserAlreadyExistsException;
////import com.WorkStudySync.exception.UserEmailNotExistException;
////import com.WorkStudySync.payload.request.UserLoginRequest;
////import com.WorkStudySync.payload.request.UserRegisterRequest;
////import com.WorkStudySync.repository.RoleRepository;
////import com.WorkStudySync.repository.UserRepository;
////import com.WorkStudySync.repository.UserRoleRepository;
////import com.WorkStudySync.service.imp.AuthorServiceImp;
////import com.WorkStudySync.util.JwtUltils;
//import com.google.gson.Gson;
//
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletResponse;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//public class AuthorService implements AuthorServiceImp {
//     @Autowired
//     private UserRepository userRepository;
//     @Autowired
//     private UserRoleRepository userRoleRepository;
//     @Autowired
//     private RoleRepository roleRepository;
//     @Autowired
//     private PasswordEncoder passwordEncoder;
// //    Giúp lấy giá trị khai báo bên application.properties.
//     @Value("${key.token.jwt}")
//     private String strKeyToken;
//
//     @Autowired
//     private JwtUltils jwtUltils;
//
//     private Gson gson = new Gson();
//
// 	@Override
// 	public String checkLogin(UserLoginRequest userLoginReq, HttpServletResponse response) {
// 		String token = "";
// 		UserEntity userEntity = userRepository.findByEmail(userLoginReq.getEmail()).orElse(null);
// 		if (userEntity != null) {
// 			// So sánh mật khẩu người dùng nhập vào với mật khẩu đã mã hóa trong cơ sở dữ
// 			// liệu
// 			if (passwordEncoder.matches(userLoginReq.getPassword(), userEntity.getPasswordHash())) 
// 			{
// 				// Lấy danh sách các role của user và chuyển đổi thành danh sách tên role
// 				List<String> roleNames = userRoleRepository.findRoleNamesByUser(userEntity);
// 				// Tạo payload để nhúng vào JWT
// 				Map<String, Object> claims = new HashMap<>();
// 				claims.put("email", userEntity.getEmail());
// 				claims.put("roles", roleNames);
//                token = jwtUltils.createToken(claims);
//
// 				// Gửi cookie lưu email (tuỳ chọn)
// 				Cookie saveUserName = new Cookie("email", userLoginReq.getEmail());
// 				saveUserName.setHttpOnly(true);
// 				saveUserName.setSecure(false);
// 				saveUserName.setPath("/");
// 				saveUserName.setMaxAge(7 * 24 * 60 * 60);
// 				response.addCookie(saveUserName);
// 			} else {
// 				throw new RuntimeException("Password is incorrect.");
// 			}
// 		} else {
// 			throw new UserEmailNotExistException("User with email " + userLoginReq.getEmail() + " does not exist.");
// 		}
//        // Sử dụng Gson để trả về thông tin user và token dưới dạng JSON
//        Map<String, Object> result = new HashMap<>();
//        result.put("token", token);
//        result.put("user", userEntity); // userEntity sẽ được Gson tự động chuyển sang JSON
//        return gson.toJson(result);
// 	}
//
///*
//* B1: Check email exists in DB if exists -> exception
//* B2: Create new UserEntity and add all info of UserRequest
//*       Todo: setRoles => Find roleEntity by Name role.
//* B3: Save this userEntity into DB.
//* */
//     @Override
//     public UserEntity registerUser(UserRegisterRequest userRegister) {
//    	// B1: Check email tồn tại chưa
//    	    if (userRepository.existsByEmail(userRegister.getEmail())) {
//    	        throw new UserAlreadyExistsException("Email " + userRegister.getEmail() + " already exists");
//    	    }
//
//    	    // B2: Tạo mới user entity
//    	    UserEntity userEntity = new UserEntity();
//    	    userEntity.setEmail(userRegister.getEmail());
//    	    userEntity.setFullName(userRegister.getFullName());
//    	    userEntity.setPasswordHash(passwordEncoder.encode(userRegister.getPassword()));
//    	    
//    	    // Parse kiểu enum
//    	    UserTypeEnum userTypeEnum;
//    	    try {
//    	        userTypeEnum = UserTypeEnum.valueOf(userRegister.getUserType().toUpperCase());
//    	    } catch (IllegalArgumentException ex) {
//    	        throw new IllegalArgumentException("Invalid userType: " + userRegister.getUserType());
//    	    }
//
//    	    userEntity.setUserType(userTypeEnum);
//    	    userEntity.setCreatedAt(LocalDateTime.now());
//
//    	    // B3: Lấy role mặc định từ DB
//    	    RoleEntity roleEntity = roleRepository.findByName("ROLE_USER")
//    	        .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
//
//    	    // B4: Save user trước để có userId (nếu bạn không dùng cascade persist)
//    	    userEntity = userRepository.save(userEntity);
//
//    	    // B5: Tạo entity trung gian user-role
//    	    UserRoleEntity userRoleEntity = new UserRoleEntity();
//    	    userRoleEntity.setUser(userEntity);
//    	    userRoleEntity.setRole(roleEntity);
//    	    userRoleEntity.setAssignedAt(LocalDateTime.now());
//
//    	    // Tạo id kết hợp
//    	    UserRoleEntityId compositeKey = new UserRoleEntityId(userEntity.getUserId(), roleEntity.getRoleId());
//    	    userRoleEntity.setId(compositeKey);
//
//    	    // B6: Lưu liên kết user-role
//    	    userRoleRepository.save(userRoleEntity);
//
//    	    // B7: Trả về user đã đăng ký (hoặc DTO tùy)
//    	    return userEntity;
//     }
//
//	@Override
//	public Map<String, Object> handleGoogleLogin(String code) {
//		// 1. Gửi code để lấy access_token và id_token
//		RestTemplate restTemplate = new RestTemplate();
//		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//		params.add("code", code);
//		params.add("client_id", "YOUR_CLIENT_ID");
//		params.add("client_secret", "YOUR_CLIENT_SECRET");
//		params.add("redirect_uri", "http://localhost:8080/oauth2/callback/google");
//		params.add("grant_type", "authorization_code");
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
//
//		ResponseEntity<Map> response = restTemplate.postForEntity("https://oauth2.googleapis.com/token", request,
//				Map.class);
//
//		String idToken = (String) response.getBody().get("id_token");
//
//		// 2. Giải mã id_token để lấy thông tin người dùng
//		Claims claims = jwtUltils.decryptTokenClaims(idToken);
//		String email = ""; // Lấy email từ dữ liệu giải mã
//		String name = "";
//		String picture = ""; // Lấy ảnh đại diện từ dữ liệu giải mã
//		UserEntity user_entity = userRepository.findByEmail(email)
//		        .orElse(new UserEntity());
//
//		if (user_entity.getUserId() == null) { // nghĩa là user mới
//			user_entity.setEmail(email);
//			user_entity.setFullName(name);
//			user_entity.setAvatar(picture);
//			user_entity = userRepository.save(user_entity);
//		}
//
//
//		// 3. Lưu hoặc cập nhật người dùng
//		UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
//			UserEntity newUser = new UserEntity();
//			newUser.setEmail(email);
//			newUser.setFullName(name);
//			newUser.setAvatar(picture);
//			return userRepository.save(newUser);
//		});
//
//		// 4. Tạo JWT để trả về cho client
//		String token = jwtUltils.createToken(user);
//
//		return Map.of("message", "Login successful", "token", token, "user",
//				Map.of("email", email, "name", name, "avatar", picture));
//	}
//}
//package com.fpm2025.user_auth_service.filter;
//
//import com.fpm2025.user_auth_service.payload.response.RoleResponse;
//import com.fpm2025.user_auth_service.util.JwtUltils;
//import com.google.gson.Gson;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class CustomJwtFilter extends OncePerRequestFilter {
//    @Autowired
//    private JwtUltils jwtUltils;
//     private Gson gson = new Gson();
//     //Hàm này kiểm tra JWT token trong header "Authorization".
//     //Nếu token hợp lệ → giải mã ra role → tạo Authentication object → set vào SecurityContext để Spring Security hiểu rằng request này đã xác thực và có quyền truy cập.
//    @Override
//	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String headerAuthen = request.getHeader("Authorization");
//        if (headerAuthen != null && headerAuthen.trim().length() > 0){
//            String token = headerAuthen.substring(7);
//            // TODO: note giải mã token
//            String data = jwtUltils.decryptToke(token);
//
//            if (data != null){
//            	RoleResponse role = gson.fromJson(data, RoleResponse.class);
//                List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
//                SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(role.getName());
//                authorityList.add(simpleGrantedAuthority);
//                // TODO: tạo chứng thực cho sercurity biết là đã hợp lệ và bypass được tất cả các filter của scurity.
//                UsernamePasswordAuthenticationToken authen =
//                        new UsernamePasswordAuthenticationToken(role.getName(), null, authorityList);
//
//                SecurityContext securityContext = SecurityContextHolder.getContext();
//                securityContext.setAuthentication(authen);
//            }
//        }
//
//        filterChain.doFilter(request,response);// Cho chạy tiếp
//    }
//}

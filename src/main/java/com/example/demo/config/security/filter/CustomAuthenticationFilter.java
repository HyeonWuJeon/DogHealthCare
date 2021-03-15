package com.example.demo.config.security.filter;




import com.example.demo.config.exception.InputNotFoundException;
import com.example.demo.member.dto.MemberDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


// AuthenticationFilter로 요청이 먼저 오게 되고, 아이디와 비밀번호를 기반으로 UserPasswordAuthenticationToken을 발급해 준다.
@Slf4j
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public CustomAuthenticationFilter(AuthenticationManager authenticationManager) {

        super.setAuthenticationManager(authenticationManager); // 요청 받음
    }

    // AuthenticaitonProvider에서 인증이 완료된 UsernamePasswordAuthenticationToken을 AuthenticationFilter로 반환하고, AuthenticationFilter에서는 LoginSuccessHandler로 전달한다.
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        UsernamePasswordAuthenticationToken authRequest;
        try{
            MemberDto.Request userVO = new ObjectMapper().readValue(request.getInputStream(), MemberDto.Request.class);
            authRequest = new UsernamePasswordAuthenticationToken(userVO.getEmail(), userVO.getPassword());
        } catch (IOException exception){
            throw new InputNotFoundException(); // LINE :: 전달 오류
        }

        setDetails(request, authRequest); // LINE :: 인증 정보 저장
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
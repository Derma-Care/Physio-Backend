package com.dermaCare.customerService.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.dermaCare.customerService.dto.AccessTokenAndRefreshToken;
import com.dermaCare.customerService.util.JwtUtil;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.RolesStore;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomCustomerDetailsService customCustomerDetailsService;

    @Autowired
    public RolesStore rolesStore;

    @Override
    public ResponseEntity<?> customerLogin(
            String userName,
            String password,
            String deviceId) {

        log.info(
                "Customer login request received username={} deviceId={}",
                userName,
                deviceId);

        Response response = new Response();

        try {

            log.debug(
                    "Setting deviceId in CustomCustomerDetailsService deviceId={}",
                    deviceId);

            customCustomerDetailsService.deviceId = deviceId;

            log.info(
                    "Authenticating user username={}",
                    userName);

            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userName,
                            password));

            log.info(
                    "Authentication successful username={}",
                    userName);

            log.debug(
                    "Fetching user roles username={}",
                    userName);

            List<String> roles = new ArrayList<>();
            roles = rolesStore.getRoles();

            log.debug(
                    "Roles fetched username={} rolesCount={}",
                    userName,
                    roles.size());

            log.debug(
                    "Generating access token username={}",
                    userName);

            String accessToken =
                    jwtUtil.generateJwtToken(
                            userName,
                            roles);

            log.debug(
                    "Generating refresh token username={}",
                    userName);

            String refreshToken =
                    jwtUtil.generateRefreshToken(
                            userName,
                            roles);

            log.info(
                    "JWT tokens generated successfully username={}",
                    userName);

            AccessTokenAndRefreshToken tokens =
                    new AccessTokenAndRefreshToken();

            tokens.setAccessToken(accessToken);
            tokens.setRefreshToken(refreshToken);
            tokens.setAccessTokenExpireTime(
                    jwtUtil.formattedTimeByZone);

            response.setMessage("Login successful");
            response.setStatus(200);
            response.setData(tokens);
            response.setSuccess(true);

            log.info(
                    "Login completed successfully username={}",
                    userName);

        } catch (Exception e) {

            log.error(
                    "Authentication failed username={} deviceId={}",
                    userName,
                    deviceId,
                    e);

            response.setMessage(e.getMessage());
            response.setStatus(500);
            response.setSuccess(false);
        }

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

    @Override
    public ResponseEntity<Response>
    requestForNewJwtTokenByRefreshToken(
            String refreshToken) {

        log.info(
                "Refresh token request received");

        try {

            log.debug(
                    "Validating refresh token");

            Response res =
                    jwtUtil.validateAndGenerateRefreshToken(
                            refreshToken);

            log.info(
                    "Refresh token validated successfully status={}",
                    res.getStatus());

            return ResponseEntity
                    .status(res.getStatus())
                    .body(res);

        } catch (Exception e) {

            log.error(
                    "Refresh token validation failed",
                    e);

            Response response = new Response();
            response.setMessage(e.getMessage());
            response.setStatus(500);

            return ResponseEntity
                    .status(response.getStatus())
                    .body(response);
        }
    }
}
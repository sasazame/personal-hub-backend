package com.zametech.todoapp.infrastructure.security;

import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        // For OAuth users (null password), return a dummy password that will never match
        // This prevents information leakage about account existence and auth method
        String password = user.getPassword();
        if (password == null) {
            // Use a secure random string that will never match any user input
            password = "$2a$10$dummypasswordthatwillnevermatchanyuserinput";
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(password)
                .authorities(new ArrayList<>())
                .disabled(!user.isEnabled())
                .build();
    }
}
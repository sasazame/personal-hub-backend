package com.zametech.personalhub.infrastructure.security;

import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

        // Assign default USER role to all users
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(password)
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .build();
    }
}
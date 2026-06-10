package com.nti.nti_backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + email
                        )
                );
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }
}
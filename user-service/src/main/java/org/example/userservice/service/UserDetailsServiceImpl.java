package org.example.userservice.service;

import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Auth auth = authRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        User user = userRepository.findByAuth_Id(auth.getId()).orElse(null);
        return new CustomUserDetails(auth, user);
    }
}

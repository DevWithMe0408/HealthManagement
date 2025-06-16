package org.example.userservice.service;

import org.example.userservice.entity.Auth;
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

    @Override
    @Transactional // Quan trọng để load User entity nếu có lazy loading
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Auth auth = authRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // CustomUserDetails sẽ tự động lấy thông tin User nếu Auth có mapping tới User
        // và bạn đã fetch nó (ví dụ, nhờ @Transactional hoặc EAGER fetch)
        return new CustomUserDetails(auth);

        // Hoặc nếu bạn không muốn Auth entity chứa User trực tiếp,
        // bạn có thể query User riêng và truyền vào constructor của CustomUserDetails:
        // User user = userRepository.findByAuthId(auth.getId()).orElse(null);
        // if (user != null) {
        //     return new CustomUserDetails(user.getId(), auth.getUsername(), auth.getEmail(), auth.getPassword(),
        //                                auth.getRole().name(), user.getName(), user.getPhoneNumber());
        // } else {
        //     // Xử lý trường hợp không tìm thấy User tương ứng với Auth (không nên xảy ra nếu dữ liệu nhất quán)
        //     throw new UsernameNotFoundException("User profile not found for auth: " + username);
        // }
    }
}
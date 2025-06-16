package org.example.userservice.security;

import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails  implements UserDetails {
    private Long id; // Có thể là user_id từ bảng users
    private String username; // Từ bảng auth
    private String password; // Từ bảng auth (đã mã hóa)
    private String email;    // Từ bảng auth
    private Collection<? extends GrantedAuthority> authorities;

    // Thêm các trường từ bảng User nếu bạn muốn truy cập trực tiếp qua Principal
    private String name; // Từ bảng users
    private String phoneNumber; // Từ bảng users
    // ... các trường khác của User ...

    // Constructor nhận vào Auth entity (và có thể cả User entity nếu bạn join sẵn)
    public CustomUserDetails(Auth auth) {
        this.username = auth.getUsername();
        this.password = auth.getPassword();
        this.email = auth.getEmail();
        // Chuyển đổi Role enum thành GrantedAuthority
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(auth.getRole().name()));

        // Nếu Auth entity có tham chiếu đến User entity (ví dụ @OneToOne User user)
        if (auth.getUser() != null) {
            User user = auth.getUser();
            this.id = user.getId();
            this.name = user.getName();
            this.phoneNumber = user.getPhone();
            // ... gán các trường khác từ User entity ...
        } else {
            // Xử lý trường hợp user chưa được liên kết đầy đủ hoặc không có thông tin user
            // (ví dụ, trong quá trình đăng ký Auth được tạo trước)
            // Hoặc nếu bạn muốn lazy load User, thì không gán ở đây.
        }
    }

    // Hoặc một constructor khác nếu bạn query User và Auth riêng
    public CustomUserDetails(Long id, String username, String email, String password, String role, String name, String phoneNumber /*, ...các trường khác của User... */) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
        this.name = name;
        this.phoneNumber = phoneNumber;
        // ...
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // Trả về username dùng để đăng nhập (thường là username từ bảng auth)
        return username;
    }

    // Các phương thức khác của UserDetails (thường trả về true cho user active)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Hoặc dựa trên một trường 'active' trong DB
    }

    // Thêm các getter cho thông tin User mà bạn muốn truy cập
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    // ...
}

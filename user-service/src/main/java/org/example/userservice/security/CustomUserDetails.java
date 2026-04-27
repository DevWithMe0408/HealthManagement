package org.example.userservice.security;

import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private String id;          // user.id (UUID)
    private String username;    // auth.username
    private String password;    // auth.password (encoded)
    private String email;       // auth.email
    private Collection<? extends GrantedAuthority> authorities;

    private String name;        // user.name
    private String phoneNumber; // user.phone
    private Auth auth;

    /**
     * Constructor used when only Auth is loaded. id stays null until a User is also passed.
     */
    public CustomUserDetails(Auth auth) {
        this.auth = auth;
        this.username = auth.getUsername();
        this.password = auth.getPassword();
        this.email = auth.getEmail();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(auth.getRole().name()));
    }

    /**
     * Constructor used when both Auth and the linked User are loaded.
     */
    public CustomUserDetails(Auth auth, User user) {
        this(auth);
        if (user != null) {
            this.id = user.getId();
            this.name = user.getName();
            this.phoneNumber = user.getPhone();
        }
    }

    public CustomUserDetails(String id, String username, String email, String password, String role, String name, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
        this.name = name;
        this.phoneNumber = phoneNumber;
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
        return username;
    }

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
        return true;
    }

    public String getId() {
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

    public Auth getAuth() {
        return auth;
    }
}

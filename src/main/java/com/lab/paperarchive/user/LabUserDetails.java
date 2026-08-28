package com.lab.paperarchive.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class LabUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final Role role;
    private final Status status;

    public LabUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.name = user.getName();
        this.role = user.getRole();
        this.status = user.getStatus();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** SUSPENDED(정지) → LockedException */
    @Override
    public boolean isAccountNonLocked() {
        return status != Status.SUSPENDED;
    }

    /** PENDING(승인대기) → DisabledException */
    @Override
    public boolean isEnabled() {
        return status == Status.ACTIVE;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public Long getUserId() {
        return id;
    }
}

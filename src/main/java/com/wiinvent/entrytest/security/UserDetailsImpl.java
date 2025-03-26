package com.wiinvent.entrytest.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wiinvent.entrytest.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor
@Builder
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String email;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String fullName;
    private final Integer lotusPoints;

    public static UserDetailsImpl createFromUser(User user) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        return new UserDetailsImpl(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            authorities,
            user.getFullName(),
            user.getLotusPoints()
        );
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static UserDetailsImplBuilder builder() {
        return new UserDetailsImplBuilder();
    }

    public static class UserDetailsImplBuilder {
        private Long id;
        private String username;
        private String email;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private String fullName;
        private Integer lotusPoints;
        
        public UserDetailsImplBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public UserDetailsImplBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public UserDetailsImplBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public UserDetailsImplBuilder password(String password) {
            this.password = password;
            return this;
        }
        
        public UserDetailsImplBuilder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }
        
        public UserDetailsImplBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }
        
        public UserDetailsImplBuilder lotusPoints(Integer lotusPoints) {
            this.lotusPoints = lotusPoints;
            return this;
        }
        
        public UserDetailsImpl build() {
            return new UserDetailsImpl(id, username, email, password, authorities, fullName, lotusPoints);
        }
    }
} 
package com.wfp.workflow;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

final class TestRoles {

    static final List<GrantedAuthority> ADMIN =
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
    static final List<GrantedAuthority> USER = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private TestRoles() {
    }
}

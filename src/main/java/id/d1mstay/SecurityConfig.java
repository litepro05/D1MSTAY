package id.d1mstay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService users(AdminRepository admins) {
        return username -> admins.findByUsername(username)
            .map(a -> User.withUsername(a.getUsername()).password(a.getPasswordHash()).roles("ADMIN").build())
            .orElseThrow(() -> new UsernameNotFoundException("Akun tidak ditemukan."));
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/app.css", "/app.js", "/icons.svg", "/favicon.ico", "/brand/**", "/health", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .build();
    }

    @Bean
    CommandLineRunner createAdmin(AdminRepository admins, PasswordEncoder encoder,
            @Value("${d1mstay.admin.username}") String username,
            @Value("${d1mstay.admin.password}") String password) {
        // Akun dibuat sekali. Menjalankan ulang aplikasi tidak mereset kata sandi.
        return args -> {
            if (admins.count() == 0) {
                var admin = new AdminAccount();
                admin.setUsername(username);
                admin.setPasswordHash(encoder.encode(password));
                admins.save(admin);
            }
        };
    }
}

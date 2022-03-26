package com.beloved.config;

import com.alibaba.fastjson.JSONObject;
import com.beloved.filter.LoginFilter;
import com.beloved.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

//    @Bean
//    public UserDetailsService userDetailsService() {
//        InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager();
//        userDetailsManager.createUser(User.withUsername("admin").password("{noop}123").roles("admin").build());
//        return userDetailsManager;
//    }

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }

    /**
     * 暴露 AuthenticationManager 到工厂
     * @return
     * @throws Exception
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // 自定义 LoginFilter 注入
    @Bean
    public LoginFilter loginFilter() throws Exception {
        LoginFilter loginFilter = new LoginFilter();

        // 指定认证 url
        loginFilter.setFilterProcessesUrl("/login");

        // 指定接收 key
        loginFilter.setUsernameParameter("uname");
        loginFilter.setPasswordParameter("pwd");

        loginFilter.setAuthenticationManager(authenticationManagerBean());

        // 认证成功处理器
        loginFilter.setAuthenticationSuccessHandler(((request, response, authentication) -> {
            JSONObject result = new JSONObject();
            result.put("msg", "登录成功");
            result.put("用户信息", authentication.getPrincipal());
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println(result);
        }));

        // 认证失败处理器
        loginFilter.setAuthenticationFailureHandler(((request, response, exception) -> {
            JSONObject result = new JSONObject();
            result.put("msg", "登录失败：" + exception.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println(result);
        }));

        return loginFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .anyRequest().authenticated() //所有请求必须认证
                .and()
                .formLogin()
                .and()
                // 未认证异常处理
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    JSONObject result = new JSONObject();
                    result.put("msg", "尚未认证：" + authException.getMessage());
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().println(result);
                })
                .and()
                .logout()
                .logoutUrl("/logout")
                // 注销登录处理
                .logoutSuccessHandler(((request, response, authentication) -> {
                    JSONObject result = new JSONObject();
                    result.put("msg", "注销成功");
                    result.put("用户信息", authentication.getPrincipal());
                    response.setStatus(HttpStatus.OK.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().println(result);
                }))
                .and()
                .csrf().disable();

        /*
         * addFilterAt：替换过滤器链中的某个 filter
         * addFilterBefore：放在过滤器链中某个 filter 之前
         * addFilterAfter：放在过滤器链中某个 filter 之后
         */
        http.addFilterAt(loginFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}

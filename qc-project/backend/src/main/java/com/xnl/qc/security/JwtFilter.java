package com.xnl.qc.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    /**
     * 完全公开、无需 JWT 的路径（精确匹配）
     * 注意：Controller 已加 /api 前缀，context-path 已去掉，
     *       req.getRequestURI() 直接返回 /api/auth/login 等完整路径
     */
    private static final Set<String> PUBLIC_EXACT = Set.of(
        "/api/auth/login",
        "/api/nurses/list"
    );

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        // 放行 OPTIONS 预检请求（CORS）
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String uri = req.getRequestURI();

        // 放行公开接口
        if (PUBLIC_EXACT.contains(uri)) {
            chain.doFilter(req, res);
            return;
        }

        // 校验 Authorization 头
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("[JwtFilter] 缺少 Authorization header，URI: {}", uri);
            json401(res, "未授权，请先登录");
            return;
        }

        String token = header.substring(7).trim();

        // 校验 token 合法性
        if (!jwtUtil.validate(token)) {
            log.warn("[JwtFilter] Token 无效或已过期，URI: {}", uri);
            json401(res, "Token 已过期或无效，请重新登录");
            return;
        }

        // 解析并写入 request attribute
        String employeeId = jwtUtil.getEmployeeId(token);
        String name       = jwtUtil.getName(token);
        boolean admin     = jwtUtil.isAdmin(token);

        req.setAttribute("employeeId", employeeId);
        req.setAttribute("name",       name);
        req.setAttribute("admin",      admin);

        log.debug("[JwtFilter] 通过验证 - employeeId: {}, admin: {}, URI: {}", employeeId, admin, uri);

        chain.doFilter(req, res);
    }

    private void json401(HttpServletResponse res, String msg) throws IOException {
        res.setStatus(401);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\":401,\"message\":\"" + msg + "\"}");
    }
}

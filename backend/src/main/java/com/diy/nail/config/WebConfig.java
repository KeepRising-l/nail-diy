package com.diy.nail.config;

import com.diy.nail.common.BusinessException;
import com.diy.nail.common.ResultCode;
import com.diy.nail.entity.Material;
import com.diy.nail.mapper.MaterialMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.file.Paths;

/**
 * Web 全局配置：跨域 + 上传文件静态映射
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final MaterialMapper materialMapper;

    @Value("${nail.upload.dir}")
    private String uploadDir;

    /** 开发环境跨域放行（生产由 Nginx 同源部署） */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** uploads/ 目录下的文件通过 /uploads/** 访问 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }

    /** 兼容旧数据：阻止通过已知 /uploads URL 绕过 UGC 审核状态。 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                return preHandleUpload(request);
            }
        }).addPathPatterns("/uploads/**");
    }

    boolean preHandleUpload(HttpServletRequest request) {
        String imageUrl = canonicalUploadPath(request);
        Material protectedMaterial = materialMapper.selectProtectedLegacyImage(imageUrl);
        if (protectedMaterial == null) {
            return true;
        }
        if (Integer.valueOf(1).equals(protectedMaterial.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean owner = authentication != null && authentication.getPrincipal() instanceof Long userId
                && userId.equals(protectedMaterial.getCreatorId());
        boolean operator = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_MERCHANT".equals(role) || "ROLE_ADMIN".equals(role));
        if (!owner && !operator) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return true;
    }

    private String canonicalUploadPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            if (!path.startsWith(contextPath + "/")) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            path = path.substring(contextPath.length());
        }
        if (!path.startsWith("/uploads/")) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String relativePath = path.substring("/uploads/".length());
        String[] segments = relativePath.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
                    || !segment.matches("[A-Za-z0-9._-]+")) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
        }
        return path;
    }
}

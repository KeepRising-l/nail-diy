package com.diy.nail.config;

import com.diy.nail.common.BusinessException;
import com.diy.nail.mapper.MaterialMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebConfigSecurityTest {

    private final MaterialMapper materialMapper = mock(MaterialMapper.class);
    private final WebConfig webConfig = new WebConfig(materialMapper);

    @Test
    void canonicalUploadPathIsCheckedAgainstStoredUrl() {
        HttpServletRequest request = request("/app/uploads/seed/pat_bow.png", "/app");

        assertTrue(webConfig.preHandleUpload(request));

        verify(materialMapper).selectProtectedLegacyImage("/uploads/seed/pat_bow.png");
    }

    @Test
    void encodedOrNonCanonicalUploadPathIsRejectedBeforeLookup() {
        HttpServletRequest encoded = request("/uploads/20261009/ex%61mple.png", "");
        HttpServletRequest traversal = request("/uploads/seed/../private.png", "");

        assertEquals(404, assertThrows(BusinessException.class,
                () -> webConfig.preHandleUpload(encoded)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> webConfig.preHandleUpload(traversal)).getCode());
        verifyNoInteractions(materialMapper);
    }

    private HttpServletRequest request(String uri, String contextPath) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn(contextPath);
        return request;
    }
}

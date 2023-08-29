package com.koproxy

import com.koproxy.utils.ExcludeServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.util.AntPathMatcher
import org.springframework.util.PathMatcher

@ExtendWith(MockitoExtension::class)
internal class ExcludeServiceImplTest {

    @InjectMocks
    lateinit var excludeService: ExcludeServiceImpl

    private val pathMatcher: PathMatcher = AntPathMatcher()

    @Test
    fun getExcludePattern() = runBlocking {
        ReflectionTestUtils.setField(excludeService, "frontendUrl", "/ui/cross/somedomain")
        ReflectionTestUtils.setField(excludeService, "backendUrl", "/ui-api-web/cross/somedomain")
        val excludePatterns = excludeService.getExcludePattern()
        for (exclude in excludePatterns) {
            if (!pathMatcher.isPattern(exclude)) {
                assertTrue(false)
            }
        }
    }
}

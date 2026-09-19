package org.cangnova.cangjie.cfir.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.config.MockSupportKind

/** 验证 session 组件访问器始终以声明的接口类型作为注册键。 */
class CfirSessionComponentAccessorTest {
    @Test
    fun `registered interface implementation replaces default component`() {
        val session = TestSession()
        val configuredProvider = object : CfirApiLevelProvider {
            override val projectApiLevel: Int = 20
        }

        session.register(CfirApiLevelProvider::class, configuredProvider)

        assertSame(configuredProvider, session.apiLevelProvider)
    }

    @Test
    fun `conditional compilation environment is explicit and backend neutral`() {
        val session = TestSession()
        val settings = ExplicitCfirConditionalCompilationSettings(
            backend = "cjnative",
            arch = "x86_64",
            os = "Windows",
            cjcVersion = "1.0.5",
            debug = false,
            test = true,
            userDefined = mapOf("feature" to "enabled"),
        )

        session.register(CfirConditionalCompilationSettings::class, settings)

        assertSame(settings, session.conditionalCompilationSettings)
        assertEquals("enabled", session.conditionalCompilationSettings.userDefined["feature"])
    }

    @Test
    fun `mock capability follows official test and mock options`() {
        val session = TestSession()
        val settings = CfirMockSettingsComponent(
            enableCompileTest = true,
            mockSupportKind = MockSupportKind.DEFAULT,
        )

        session.register(CfirMockSettingsComponent::class, settings)

        assertTrue(session.mockSettings.enableCompileTest)
        assertTrue(session.mockSettings.mockCompatibleIfNeeded)
        assertTrue(session.mockSettings.mockCompatible)
        assertFalse(session.mockSettings.mockCompileOnly)
    }

    @Test
    fun `mock capability does not infer test mode from source path`() {
        val session = TestSession()

        assertFalse(session.mockSettings.enableCompileTest)
        assertFalse(session.mockSettings.mockCompatible)
        assertFalse(session.mockSettings.mockCompileOnly)
    }

    /** 测试专用的最小源码 session。 */
    private class TestSession : CfirSession(Kind.Source)
}

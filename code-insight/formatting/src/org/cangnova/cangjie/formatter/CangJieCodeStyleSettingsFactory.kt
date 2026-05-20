package org.cangnova.cangjie.formatter

import com.intellij.lang.Language
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsService
import com.intellij.psi.codeStyle.CodeStyleSettingsServiceListener
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.psi.codeStyle.DocCommentSettings
import com.intellij.psi.codeStyle.FileTypeIndentOptionsFactory
import com.intellij.psi.codeStyle.LanguageCodeStyleProvider
import org.cangnova.cangjie.lang.CangJieLanguage

/**
 * 共享 formatter 在 headless/LSP 场景下的代码样式注册入口。
 *
 * IDE 会通过 `LanguageCodeStyleSettingsProvider` 暴露完整设置 UI；
 * LSP 没有该 UI 和 plugin XML，因此这里直接把仓颉语言的 common/custom
 * code style settings 注册到临时 `CodeStyleSettings` 容器中。
 */
object CangJieCodeStyleSettingsFactory {
    fun createDefaultSettings(): CodeStyleSettings =
        HeadlessCangJieCodeStyleSettings().also { settings ->
            ensureCodeStyleSettingsServiceRegistered()
            registerCangJieSettings(settings)
        }

    fun registerCangJieSettings(settings: CodeStyleSettings) {
        settings.registerCommonSettings(CangJieHeadlessLanguageCodeStyleProvider)
        settings.registerCustomSettings(CangJieHeadlessLanguageCodeStyleProvider)
    }

    private object CangJieHeadlessLanguageCodeStyleProvider : LanguageCodeStyleProvider {
        override fun getLanguage(): Language = CangJieLanguage

        override fun getDefaultCommonSettings(): CommonCodeStyleSettings =
            CangJieCommonCodeStyleSettings().apply {
                initIndentOptions()
            }

        override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings =
            CangJieCodeStyleSettings(settings)

        override fun getDocCommentSettings(settings: CodeStyleSettings): DocCommentSettings =
            DocCommentSettings.DEFAULTS

        override fun getSupportedFields(): Set<String> = emptySet()
    }

    /**
     * 共享 formatter 运行在 headless/LSP/单测环境时，不能假设 IntelliJ 的
     * `CodeStyleSettingsService` 已经按 IDE plugin 方式注册完成。
     *
     * 这里显式关闭构造阶段的 service 依赖，随后再由 [registerCangJieSettings]
     * 注册仓颉语言自己的 common/custom settings，保持规则所有权仍在本模块。
     */
    private class HeadlessCangJieCodeStyleSettings : CodeStyleSettings(false, false)

    /**
     * 单测、LSP、headless CLI 里没有 plugin.xml 驱动的 application service 注册，
     * 但 IntelliJ code-style 框架在注册 common/custom settings 时仍会强依赖这个 service。
     *
     * 因此共享 formatter 自己在 MockApplication 中补齐最小可用 service，
     * 保证规则依旧只从本模块暴露。
     */
    private fun ensureCodeStyleSettingsServiceRegistered() {
        val application = ApplicationManager.getApplication() as? MockApplication ?: return
        if (application.getService(CodeStyleSettingsService::class.java) == null) {
            application.registerService(CodeStyleSettingsService::class.java, HeadlessCodeStyleSettingsService())
        }
    }

    private class HeadlessCodeStyleSettingsService : CodeStyleSettingsService {
        override fun addListener(listener: CodeStyleSettingsServiceListener, parentDisposable: Disposable?) {
        }

        override fun getFileTypeIndentOptionsFactories(): List<FileTypeIndentOptionsFactory> = emptyList()

        override fun getCustomCodeStyleSettingsFactories(): List<LanguageCodeStyleProvider> =
            listOf(CangJieHeadlessLanguageCodeStyleProvider)

        override fun getLanguageCodeStyleProviders(): List<LanguageCodeStyleProvider> =
            listOf(CangJieHeadlessLanguageCodeStyleProvider)
    }
}

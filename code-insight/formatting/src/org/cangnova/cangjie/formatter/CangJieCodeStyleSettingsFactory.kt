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
    /**
     * 创建带仓颉 common/custom settings 的默认代码风格配置。
     */
    fun createDefaultSettings(): CodeStyleSettings =
        HeadlessCangJieCodeStyleSettings().also { settings ->
            ensureCodeStyleSettingsServiceRegistered()
            registerCangJieSettings(settings)
        }

    /**
     * 将仓颉 common/custom settings 注册到指定配置容器。
     */
    fun registerCangJieSettings(settings: CodeStyleSettings) {
        settings.registerCommonSettings(CangJieHeadlessLanguageCodeStyleProvider)
        settings.registerCustomSettings(CangJieHeadlessLanguageCodeStyleProvider)
    }

    /**
     * Headless 环境中提供仓颉代码风格设置实例的轻量 provider。
     */
    private object CangJieHeadlessLanguageCodeStyleProvider : LanguageCodeStyleProvider {
        /**
         * 返回该 provider 覆盖的语言。
         */
        override fun getLanguage(): Language = CangJieLanguage

        /**
         * 创建仓颉 common settings 默认实例。
         */
        override fun getDefaultCommonSettings(): CommonCodeStyleSettings =
            CangJieCommonCodeStyleSettings().apply {
                initIndentOptions()
            }

        /**
         * 创建仓颉 custom settings 默认实例。
         */
        override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings =
            CangJieCodeStyleSettings(settings)

        /**
         * 仓颉当前复用 IntelliJ 默认文档注释设置。
         */
        override fun getDocCommentSettings(settings: CodeStyleSettings): DocCommentSettings =
            DocCommentSettings.DEFAULTS

        /**
         * Headless provider 不暴露 UI 字段集合。
         */
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

    /**
     * Headless formatter 使用的最小 CodeStyleSettingsService。
     */
    private class HeadlessCodeStyleSettingsService : CodeStyleSettingsService {
        /**
         * Headless 服务不维护设置监听器。
         */
        override fun addListener(listener: CodeStyleSettingsServiceListener, parentDisposable: Disposable?) {
        }

        /**
         * 不提供文件类型级缩进选项工厂。
         */
        override fun getFileTypeIndentOptionsFactories(): List<FileTypeIndentOptionsFactory> = emptyList()

        /**
         * 返回仓颉 custom settings provider。
         */
        override fun getCustomCodeStyleSettingsFactories(): List<LanguageCodeStyleProvider> =
            listOf(CangJieHeadlessLanguageCodeStyleProvider)

        /**
         * 返回仓颉 language code style provider。
         */
        override fun getLanguageCodeStyleProviders(): List<LanguageCodeStyleProvider> =
            listOf(CangJieHeadlessLanguageCodeStyleProvider)
    }
}

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
import org.cangnova.cangjie.supportsCjmpFeature

/** CJO 消费者对格式版本的策略；编译语义与反编译可读性由各自 owner 决定。 */
public sealed interface CjoFormatVersionPolicy {
    public fun checkCommonPart(packageName: String, version: CjoModuleVersion?): CfirCjmpLoadDiagnostic?

    /**
     * 延迟到具体消费端 session 校验。
     *
     * 二进制库 session 可以被多个语言版本不同的源码 session 共用，不能用库 session 的语言设置
     * 提前拒绝 common-part CJO；consumer-aware symbol provider 会按实际消费端执行同一格式门禁。
     */
    public data object DeferredToConsumerSession : CjoFormatVersionPolicy {
        override fun checkCommonPart(packageName: String, version: CjoModuleVersion?): CfirCjmpLoadDiagnostic? = null
    }

    /** 按本次编译使用的语言版本选择官方格式兼容行为。 */
    public data class Compilation(
        public val languageVersionSettings: LanguageVersionSettings,
    ) : CjoFormatVersionPolicy {
        override fun checkCommonPart(packageName: String, version: CjoModuleVersion?): CfirCjmpLoadDiagnostic? =
            if (languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.CjoFormatVersionCompatibility)) {
                CjmpCommonPartLoadGate.checkFormatVersion(packageName, version)
            } else {
                null
            }
    }

    /** 反编译器按文件 stub 的格式能力单独判定，不借用编译语言版本语义。 */
    public data object Decompilation : CjoFormatVersionPolicy {
        override fun checkCommonPart(packageName: String, version: CjoModuleVersion?): CfirCjmpLoadDiagnostic? = null
    }
}

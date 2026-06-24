package org.cangnova.cangjie.cfir.analysis.extensions

import org.cangnova.cangjie.cfir.analysis.checkers.LanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckers
import org.cangnova.cangjie.cfir.extensions.CfirExtension
import org.cangnova.cangjie.cfir.extensions.CfirExtensionPointName
import org.cangnova.cangjie.cfir.extensions.CfirExtensionService
import org.cangnova.cangjie.cfir.session.CfirSession
import kotlin.reflect.KClass

/**
 * 对齐 Kotlin `FirAdditionalCheckersExtension`。
 *
 * low-level-api-cfir 需要通过统一扩展点把附加 checker 挂入声明、表达式、类型检查流程，
 * 不能继续在 IDE 层私有拼接。
 *
 * @param session 扩展实例所属的 CFIR session。
 */
abstract class CfirAdditionalCheckersExtension(
    session: CfirSession,
) : CfirExtension(session) {
    /** 附加 checker 扩展点的名称注册容器。 */
    companion object {
        /** 当前扩展点在 `CfirExtensionService` 中使用的稳定名称。 */
        val NAME: CfirExtensionPointName = CfirExtensionPointName("ExtensionCheckers")
    }

    /** 扩展提供的声明 checker 分组，默认不附加任何声明 checker。 */
    open val declarationCheckers: DeclarationCheckers = DeclarationCheckers.EMPTY

    /** 扩展提供的表达式 checker 分组，默认不附加任何表达式 checker。 */
    open val expressionCheckers: ExpressionCheckers = ExpressionCheckers.EMPTY

    /** 扩展提供的类型 checker 分组，默认不附加任何类型 checker。 */
    open val typeCheckers: TypeCheckers = TypeCheckers.EMPTY

    /** 扩展提供的语言版本设置 checker 分组，默认不附加任何全局 checker。 */
    open val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers = LanguageVersionSettingsCheckers.EMPTY

    /** 当前扩展在扩展服务中的名称。 */
    override val name: CfirExtensionPointName
        get() = NAME

    /** 当前扩展点允许注册的扩展实现类型。 */
    override val extensionType: KClass<out CfirAdditionalCheckersExtension>
        get() = CfirAdditionalCheckersExtension::class

    /** 基于 session 创建附加 checker 扩展实例的工厂接口。 */
    fun interface Factory {
        /** 为指定 session 创建一个附加 checker 扩展实例。 */
        fun create(session: CfirSession): CfirAdditionalCheckersExtension
    }
}

/** 从扩展服务中读取已经注册的附加 checker 扩展实例列表。 */
val CfirExtensionService.additionalCheckers: List<CfirAdditionalCheckersExtension>
    get() = getExtensions(CfirAdditionalCheckersExtension.NAME)

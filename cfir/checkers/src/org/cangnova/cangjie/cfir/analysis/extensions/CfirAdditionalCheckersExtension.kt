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
 */
abstract class CfirAdditionalCheckersExtension(
    session: CfirSession,
) : CfirExtension(session) {
    companion object {
        val NAME: CfirExtensionPointName = CfirExtensionPointName("ExtensionCheckers")
    }

    open val declarationCheckers: DeclarationCheckers = DeclarationCheckers.EMPTY
    open val expressionCheckers: ExpressionCheckers = ExpressionCheckers.EMPTY
    open val typeCheckers: TypeCheckers = TypeCheckers.EMPTY
    open val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers = LanguageVersionSettingsCheckers.EMPTY

    override val name: CfirExtensionPointName
        get() = NAME

    override val extensionType: KClass<out CfirAdditionalCheckersExtension>
        get() = CfirAdditionalCheckersExtension::class

    fun interface Factory {
        fun create(session: CfirSession): CfirAdditionalCheckersExtension
    }
}

val CfirExtensionService.additionalCheckers: List<CfirAdditionalCheckersExtension>
    get() = getExtensions(CfirAdditionalCheckersExtension.NAME)

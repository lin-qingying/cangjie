package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageFeatureSupportStatus
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.featureSupportStatus
import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextDependentRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.Renderer
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * CFIR 诊断参数渲染器集合。
 */
object CfirDiagnosticRenderers {
    /**
     * 按 Kotlin `LanguageFeatureMessageRenderer` 的职责渲染完整版本原因。
     *
     * 诊断载荷必须保留 settings：仅渲染 feature 名称会把"语言版本过低、
     * 显式关闭、实验特性未开启"全部错误地压成同一条消息。
    */
    val LANGUAGE_FEATURE_SUPPORT = Renderer<Pair<LanguageFeature, LanguageVersionSettings>> { (feature, settings) ->
        val sinceVersion = feature.sinceVersion
        val supportStatus = settings.featureSupportStatus(feature)
        val reason = when {
            feature.testOnly -> "unsupported."
            sinceVersion == null ->
                "experimental and is not enabled by the current language settings; no stability guarantees are provided."
            supportStatus == LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION ->
                "only available since language version ${sinceVersion.versionString}"
            supportStatus == LanguageFeatureSupportStatus.DISABLED -> "disabled."
            else -> "not supported by the current language settings"
        }
        buildString {
            append("The feature \"")
            append(feature.presentableName)
            append("\" is ")
            append(reason)
            feature.hintUrl?.let { append(" (see: ").append(it).append(')') }
        }
    }


    /**
     * 渲染单个 cone 类型。
     */
    val RENDER_TYPE = ContextDependentRenderer { type: ConeCangJieType, _ ->
        type.toString()
    }

    /**
     * 渲染 cone 类型集合。
     */
    val RENDER_TYPE_LIST = ContextDependentRenderer { types: Collection<ConeCangJieType>, context ->
        types.joinToString(", ") { RENDER_TYPE.render(it, context) }
    }

    /**
     * 渲染 CFIR 符号的声明名。
     */
    val DECLARATION_NAME = Renderer { symbol: CfirBasedSymbol<*> ->
        when (symbol) {
            is CfirCallableSymbol<*> -> symbol.name.asString()
            is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
            is CfirTypeParameterSymbol -> symbol.name.asString()
            else -> return@Renderer "???"
        }
    }

    /**
     * 渲染 CFIR 符号声明名集合。
     */
    val DECLARATION_NAME_LIST = ContextDependentRenderer { symbols: Collection<CfirBasedSymbol<*>>, context ->
        symbols.joinToString(", ") { DECLARATION_NAME.render(it, context) }
    }
}

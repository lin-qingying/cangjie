package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.components.CaTypeInformationProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toClassLikeSymbol
import org.cangnova.cangjie.cfir.types.withoutAbbreviation

/**
 * 对齐 Kotlin `KaFirTypeInformationProvider` 的职责边界。
 *
 * 这里专门暴露“从一个现有公开类型再观察其信息”的能力，
 * 不承担类型构造、类型替换或类型关系判断。
 */
internal class CaCfirTypeInformationProvider(
    /**
     * 延迟取得当前 CFIR Analysis session，类型信息查询需要底层 CFIR session。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeInformationProvider, CaCfirSessionComponent {
    /**
     * 判断公开类型是否代表底层错误类型。
     */
    override val CaType.isErrorType: Boolean
        get() = withValidityAssertion {
            when (this@isErrorType) {
                is CaCfirType -> coneType.isError
                else -> error("Only CFIR public types can expose error flag: ${this@isErrorType::class.simpleName}")
            }
        }

    /**
     * 返回展开类型别名并移除 abbreviation 后的公开类型。
     */
    override val CaType.fullyExpandedType: CaType
        get() = withValidityAssertion {
            when (this@fullyExpandedType) {
                is CaCfirType -> coneType
                    .fullyExpandedType(analysisSession.cfirSession)
                    .withoutAbbreviation()
                    .asCaType(analysisSession)
                else -> error("Only CFIR public types can expose fullyExpandedType: ${this@fullyExpandedType::class.simpleName}")
            }
        }

    /**
     * 返回类型指向的 class-like 符号。
     */
    override val CaType.classLikeSymbol: CaClassLikeSymbol?
        get() = withValidityAssertion {
            when (this@classLikeSymbol) {
                is CaClassLikeType -> symbol
                is CaPrimitiveType -> null
                is CaCfirType -> coneType.toClassLikeSymbol(analysisSession.cfirSession)
                    ?.let(analysisSession.cfirSymbolBuilder.classifierBuilder::buildClassLikeSymbol)
                else -> error("Only CFIR public types can resolve class-like symbols: ${this@classLikeSymbol::class.simpleName}")
            }
        }
}

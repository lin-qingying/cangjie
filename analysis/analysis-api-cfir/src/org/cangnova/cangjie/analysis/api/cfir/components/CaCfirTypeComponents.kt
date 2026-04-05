package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeRelationChecker
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.pointers.CaTypePointer
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression

/**
 * CFIR 类型查询组件集合。
 *
 * 这一层统一负责：
 * 1. 将公开 Analysis API 的类型查询映射到 session 内部协议。
 * 2. 将类型元信息与类型关系约束在同一套 CFIR 语义模型内。
 * 3. 避免组件层直接接触 low-level facade 细节。
 */
internal class CaCfirExpressionTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionTypeProvider, CaCfirSessionComponent {
    override val CjExpression.expressionType: CaType?
        get() = withValidityAssertion {
            analysisSession.queryExpressionType(this@expressionType)?.asPublicType()
        }

    override val CjCallableDeclaration.returnType: CaType?
        get() = withValidityAssertion {
            analysisSession.queryDeclarationReturnType(this@returnType)?.asPublicType()
        }
}

/**
 * 对外暴露符号关联类型的稳定入口。
 */
internal class CaCfirTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeProvider, CaCfirSessionComponent {
    override val CaCallableSymbol.returnType: CaType?
        get() = withValidityAssertion {
            when (this@returnType) {
                is CaCfirCallableSymbolImpl -> analysisSession.queryCallableReturnType(backingSymbol)?.asPublicType()
                else -> error("仅支持通过 CFIR callable 符号查询返回类型：${this@returnType::class.simpleName}")
            }
        }

    override val CaClassLikeSymbol.defaultType: CaType
        get() = withValidityAssertion {
            when (this@defaultType) {
                is CaCfirClassLikeSymbolImpl -> analysisSession.queryClassLikeDefaultType(backingSymbol)?.asPublicType()
                    ?: error("无法为 `${classId.asString()}` 构建默认类型")
                else -> error("仅支持通过 CFIR class-like 符号查询默认类型：${this@defaultType::class.simpleName}")
            }
        }

    override val CaClassLikeSymbol.superTypes: List<CaType>
        get() = withValidityAssertion {
            when (this@superTypes) {
                is CaCfirClassLikeSymbolImpl -> analysisSession.queryClassLikeSuperTypes(backingSymbol)
                    .map { superType -> superType.asPublicType() }
                else -> error("仅支持通过 CFIR class-like 符号查询直接超类型：${this@superTypes::class.simpleName}")
            }
        }
}

/**
 * 类型指针与类型元信息组件。
 */
internal class CaCfirTypeInformationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeInformationProvider, CaCfirSessionComponent {
    override fun CaType.createPointer(): CaTypePointer<CaType> = withValidityAssertion {
        when (this@createPointer) {
            is CaCfirTypeImpl -> CaCfirTypePointer(coneType)
            else -> error("仅支持为 CFIR 类型创建类型指针：${this@createPointer::class.simpleName}")
        }
    }

    override val CaType.isErrorType: Boolean
        get() = withValidityAssertion {
            when (this@isErrorType) {
                is CaCfirTypeImpl -> coneType.isError
                else -> error("仅支持查询 CFIR 类型的错误状态：${this@isErrorType::class.simpleName}")
            }
        }

    override val CaType.classLikeSymbol: CaClassLikeSymbol?
        get() = withValidityAssertion {
            when (this@classLikeSymbol) {
                is CaCfirTypeImpl -> analysisSession.queryTypeClassLikeSymbol(coneType)?.let(analysisSession::createClassLikeSymbol)
                else -> error("仅支持查询 CFIR 类型的 class-like 符号：${this@classLikeSymbol::class.simpleName}")
            }
        }
}

/**
 * 类型关系判定组件。
 */
internal class CaCfirTypeRelationChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeRelationChecker, CaCfirSessionComponent {
    override fun CaType.isSubTypeOf(superType: CaType): Boolean = withValidityAssertion {
        val subConeType = this@isSubTypeOf.requireCfirConeType("子类型关系判断")
        val superConeType = superType.requireCfirConeType("子类型关系判断")
        analysisSession.isSubTypeOf(subConeType, superConeType)
    }

    override fun CaType.semanticallyEquals(other: CaType): Boolean = withValidityAssertion {
        val leftConeType = this@semanticallyEquals.requireCfirConeType("类型语义相等判断")
        val rightConeType = other.requireCfirConeType("类型语义相等判断")
        analysisSession.areTypesEqual(leftConeType, rightConeType)
    }
}

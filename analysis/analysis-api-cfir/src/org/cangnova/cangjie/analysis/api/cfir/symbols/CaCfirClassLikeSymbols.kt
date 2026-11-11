package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirClassLikeSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaModifierExplicitness
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * class / typealias 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirNamedClassSymbol`、`KaFirTypeAliasSymbol` 落位，
 * 将 class-like 叶子从巨型模型文件中拆出。
 */
internal class CaCfirClassSymbol private constructor(
    /**
     * class-like 声明对应的源码 PSI。
     */
    override val backingPsi: CjTypeStatement?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR class-like 符号。
     */
    override val lazyCfirSymbol: Lazy<CfirClassLikeSymbol<*>>,
) : CaClassSymbol,
    CaCfirCjBasedSymbol<CjTypeStatement, CfirClassLikeSymbol<*>>,
    CaTypeParameterOwnerSymbol,
    CaDeclarationContainerSymbol {
    /**
     * class-like 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirClassLikeSymbol<*>
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: CjTypeStatement, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirClassLikeSymbol<*>, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjTypeStatement,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * class-like CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * class-like 符号所在模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * class-like 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * class-like 对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * class-like 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    /**
     * class-like 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = visibilityExplicitness == CaModifierExplicitness.EXPLICIT

    override val visibilityExplicitness: CaModifierExplicitness
        get() = withValidityAssertion { status.asPublicVisibilityExplicitness() }

    /**
     * class-like modality。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    /**
     * class-like modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = modalityExplicitness == CaModifierExplicitness.EXPLICIT

    override val modalityExplicitness: CaModifierExplicitness
        get() = withValidityAssertion { status.asPublicModalityExplicitness() }

    /**
     * class-like 的 classId。
     */
    override val classId: ClassId?
        get() = withValidityAssertion { backingPsi?.getClassId() ?: cfirSymbol.classId }

    /**
     * class-like 名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    /**
     * class-like 类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: cfirSymbol.createCjTypeParameters(builder)
        }

    /**
     * class-like 符号位置。
     */
    override val location
        get() = withValidityAssertion { backingPsi?.location ?: getSymbolKind() }

    /**
     * class-like 的公开类别。
     *
     * 无法判定时失败；需要「不确定就返回 null」的调用方请用 [classKindOrNull]。
     */
    override val classKind: CaClassKind
        get() = classKindOrNull ?: error("Class kind is unavailable for `${name.asString()}`")

    /**
     * class-like 的公开类别；符号不可恢复或出现未分类形态时返回 `null`。
     */
    override val classKindOrNull: CaClassKind?
        get() = withValidityAssertion {
            when (val symbol = cfirSymbolOrNull) {
                null -> null
                is CfirClassSymbol -> CaClassKind.CLASS
                is CfirInterfaceSymbol -> CaClassKind.INTERFACE
                is org.cangnova.cangjie.cfir.symbols.CfirStructSymbol -> CaClassKind.STRUCT
                is org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol -> CaClassKind.ENUM
                // primitive 的合成 class-like 是名称解析与 qualifier 查找的正常形态，
                // 不是错误路径；Analysis 公开层必须能区分它而不是抛错。
                is org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol -> CaClassKind.PRIMITIVE
                else -> null.also {
                    // 未分类形态是缺陷而不是环境状态：留下可检索的日志，同时让展示层继续工作。
                    LOG.warn("Unsupported class-like CFIR symbol: ${symbol::class.simpleName}, symbol=$symbol")
                }
            }
        }

    /**
     * class-like 直接父类型列表。
     */
    override val superTypes: List<CaType>
        get() = withValidityAssertion {
            cfirSymbol.superTypesList(builder)
        }

    /**
     * 创建可按 classId 恢复 class-like 符号的 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val stableClassId = classId ?: error("Class symbol `${name}` is missing ClassId")
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(stableClassId), CaAnnotatedSymbol::class.java)
    }
}

/**
 * CFIR typealias 符号实现。
 */
internal class CaCfirTypeAliasSymbol private constructor(
    /**
     * typealias 对应的源码 PSI。
     */
    override val backingPsi: CjTypeAlias?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR typealias 符号。
     */
    override val lazyCfirSymbol: Lazy<CfirTypeAliasSymbol>,
) : CaTypeAliasSymbol,
    CaCfirCjBasedSymbol<CjTypeAlias, CfirTypeAliasSymbol>,
    CaTypeParameterOwnerSymbol {
    /**
     * typealias 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirTypeAliasSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: CjTypeAlias, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirTypeAliasSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjTypeAlias,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * typealias CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * typealias 所在模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * typealias 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * typealias 对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * typealias 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    /**
     * typealias 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = visibilityExplicitness == CaModifierExplicitness.EXPLICIT

    override val visibilityExplicitness: CaModifierExplicitness
        get() = withValidityAssertion { status.asPublicVisibilityExplicitness() }

    /**
     * typealias modality。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    /**
     * typealias modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = modalityExplicitness == CaModifierExplicitness.EXPLICIT

    override val modalityExplicitness: CaModifierExplicitness
        get() = withValidityAssertion { status.asPublicModalityExplicitness() }

    /**
     * typealias 的 classId。
     */
    override val classId: ClassId?
        get() = withValidityAssertion { backingPsi?.getClassId() ?: cfirSymbol.classId }

    /**
     * typealias 名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    /**
     * typealias 类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: cfirSymbol.createCjTypeParameters(builder)
        }

    /**
     * typealias 符号位置。
     */
    override val location
        get() = withValidityAssertion { backingPsi?.location ?: getSymbolKind() }

    /**
     * typealias 完全展开后的公开类型。
     */
    override val expandedType: CaType
        get() = withValidityAssertion {
            cfirSymbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
            val expandedConeType = (cfirSymbol.cfir as CfirTypeAlias).expandedTypeRef.coneTypeOrNull
                ?.fullyExpandedType(analysisSession.cfirSession)
                ?: error("Cannot build expanded type for `${cfirSymbol.classId.asString()}`")
            builder.typeBuilder.buildType(expandedConeType)
        }

    /**
     * 创建可按 classId 恢复 typealias 符号的 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val stableClassId = classId ?: error("Type-alias symbol `${name}` is missing ClassId")
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(stableClassId), CaAnnotatedSymbol::class.java)
    }
}

/**
 * class-like 符号形态判定的日志对象。
 */
private val LOG: Logger = Logger.getInstance("org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbols")

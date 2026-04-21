package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirClassLikeSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
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
    override val backingPsi: CjTypeStatement?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirClassLikeSymbol<*>>,
) : CaClassSymbol,
    CaCfirCjBasedSymbol<CjTypeStatement, CfirClassLikeSymbol<*>>,
    CaCfirBackedSymbol<CfirClassLikeSymbol<*>>,
    CaTypeParameterOwnerSymbol,
    CaDeclarationContainerSymbol {
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

    override val backingSymbol: CfirClassLikeSymbol<*>
        get() = cfirSymbol

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { analysisSession.renderAnnotations(this).asCaAnnotationList(token) }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val containingDeclaration: CaSymbol?
        get() = withValidityAssertion { analysisSession.findContainingDeclarationSymbol(psi) }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    override val classId: ClassId?
        get() = withValidityAssertion { backingPsi?.getClassId() ?: cfirSymbol.classId }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir.typeParameters.map { typeParameter ->
                analysisSession.createTypeParameterSymbol(typeParameter.symbol)
            })
        }

    override val location
        get() = withValidityAssertion { backingPsi?.location ?: analysisSession.locationForDeclaration(this) }

    override val classKind: CaClassKind
        get() = withValidityAssertion {
            when (cfirSymbol) {
                is CfirClassSymbol -> CaClassKind.CLASS
                is CfirInterfaceSymbol -> CaClassKind.INTERFACE
                is org.cangnova.cangjie.cfir.symbols.CfirStructSymbol -> CaClassKind.STRUCT
                is org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol -> CaClassKind.ENUM
                else -> error("Unsupported class-like symbol `${cfirSymbol::class.simpleName}`")
            }
        }

    override val superTypes: List<CaType>
        get() = withValidityAssertion {
            analysisSession.typeQueries.queryClassLikeSuperTypes(cfirSymbol).map { superType -> superType.asCaType(analysisSession) }
        }

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val stableClassId = classId ?: error("Class symbol `${name}` is missing ClassId")
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(stableClassId), CaAnnotatedSymbol::class.java)
    }
}

internal class CaCfirTypeAliasSymbol private constructor(
    override val backingPsi: CjTypeAlias?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirTypeAliasSymbol>,
) : CaTypeAliasSymbol,
    CaCfirCjBasedSymbol<CjTypeAlias, CfirTypeAliasSymbol>,
    CaCfirBackedSymbol<CfirTypeAliasSymbol>,
    CaTypeParameterOwnerSymbol {
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

    override val backingSymbol: CfirTypeAliasSymbol
        get() = cfirSymbol

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { analysisSession.renderAnnotations(this).asCaAnnotationList(token) }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val containingDeclaration: CaSymbol?
        get() = withValidityAssertion { analysisSession.findContainingDeclarationSymbol(psi) }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    override val classId: ClassId?
        get() = withValidityAssertion { backingPsi?.getClassId() ?: cfirSymbol.classId }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: cfirSymbol.cfir.typeParameters.map { typeParameter ->
                analysisSession.createTypeParameterSymbol(typeParameter.symbol)
            }
        }

    override val location
        get() = withValidityAssertion { backingPsi?.location ?: analysisSession.locationForDeclaration(this) }

    override val expandedType: CaType
        get() = withValidityAssertion {
            ((cfirSymbol.cfir as CfirTypeAlias).expandedTypeRef.coneTypeOrNull?.asCaType(analysisSession))
                ?: error("Cannot build expanded type for `${cfirSymbol.classId.asString()}`")
        }

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val stableClassId = classId ?: error("Type-alias symbol `${name}` is missing ClassId")
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(stableClassId), CaAnnotatedSymbol::class.java)
    }
}

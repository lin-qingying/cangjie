package org.cangnova.cangjie.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirOptionTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRefCopy
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRefCopy
import org.cangnova.cangjie.cfir.types.impl.CfirBasicTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirFunctionTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirImplicitTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirOptionTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirTupleTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirUserTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirVArrayTypeRefImpl
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.util.wrapIntoFileAnalysisExceptionIfNeeded
import org.cangnova.cangjie.util.wrapIntoSourceCodeAnalysisExceptionIfNeeded
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import kotlin.reflect.KClass

inline fun <R> whileAnalysing(session: CfirSession, element: CfirElement, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        session.exceptionHandler.handleExceptionOnElementAnalysis(element, throwable)
    }
}
fun CfirElement.render(): String =
   CfirRenderer().renderElementAsString(this)
val CfirElement.psi: PsiElement? get() = (source as? CjPsiSourceElement)?.psi

inline fun <R> withFileAnalysisExceptionWrapping(file: CfirFile, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        file.moduleData.session.exceptionHandler.handleExceptionOnFileAnalysis(file, throwable)
    }
}
object CfirCliExceptionHandler : CfirExceptionHandler() {
    override fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing {
        throw throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element.source)
    }

    override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing {
        throw throwable.wrapIntoFileAnalysisExceptionIfNeeded(
            file.sourceFile?.path,
            file.source,
        ) { file.sourceFileLinesMapping?.getLineAndColumnByOffset(it) }
    }
}
abstract class CfirExceptionHandler : CfirSessionComponent {
    abstract fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing
    abstract fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing
}

val CfirSession.exceptionHandler: CfirExceptionHandler by CfirSession.sessionComponentAccessor()

@Suppress("UNCHECKED_CAST")
@OptIn(CfirImplementationDetail::class)
fun <R : CfirTypeRef> R.copyWithNewSource(newSource: CjSourceElement): R {
    if (source == newSource) return this

    return when (val typeRef = this) {
        is CfirResolvedTypeRef -> buildResolvedTypeRefCopy(typeRef) {
            source = newSource
        }

        is CfirErrorTypeRef -> buildErrorTypeRefCopy(typeRef) {
            source = newSource
            partiallyResolvedTypeRef = typeRef.partiallyResolvedTypeRef?.copyWithNewSource(newSource)
        }

        is CfirUserTypeRef -> CfirUserTypeRefImpl(
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            newSource,
            typeRef.qualifier.toMutableList(),
            typeRef.typeArguments.toMutableOrEmpty(),
        )

        is CfirFunctionTypeRef -> CfirFunctionTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.parameterTypeRefs.toMutableList(),
            typeRef.returnTypeRef,
        )

        is CfirOptionTypeRef -> CfirOptionTypeRefImpl(
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            newSource,
            typeRef.componentTypeRef,
        )

        is CfirTupleTypeRef -> CfirTupleTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.elementTypeRefs.toMutableList(),
        )

        is CfirVArrayTypeRef -> CfirVArrayTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.elementTypeRef,
            typeRef.sizeLiteral,
        )

        is CfirBasicTypeRef -> CfirBasicTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.name,
        )

        is CfirImplicitTypeRef -> CfirImplicitTypeRefImpl(
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
        )

        else -> error("copyWithNewSource is not implemented for ${typeRef::class.qualifiedName}")
    } as R
}

fun ConeCangJieType.toCfirResolvedTypeRef(
    source: CjSourceElement? = null,
    delegatedTypeRef: CfirTypeRef? = null,
): CfirResolvedTypeRef {
    return when (this) {
        is ConeErrorType -> buildErrorTypeRef {
            this.source = source
            this.diagnostic = this@toCfirResolvedTypeRef.diagnostic
            this.coneType = this@toCfirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }

        else -> buildResolvedTypeRef {
            this.source = source
            this.coneType = this@toCfirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }
    }
}

val CfirElement.realPsi: PsiElement? get() = (source as? CjRealPsiSourceElement)?.psi


internal fun CfirBasedSymbol<*>.errorInLazyResolve(name: String, actualClass: KClass<*>, expected: KClass<*>): Nothing {
    errorWithAttachment("Unexpected $name. Expected is ${expected.simpleName}, but was ${actualClass.simpleName}") {
        withCfirEntry("cfirElement", cfir)
        withCfirSymbolIdEntry("cfirSymbol", this@errorInLazyResolve)
    }
}


internal fun CfirMemberDeclaration.resolvedStatus(): CfirResolvedDeclarationStatus {
    lazyResolveToPhase(CfirResolvePhase.STATUS)

    val status = status
    if (status !is CfirResolvedDeclarationStatus) {
        symbol.errorInLazyResolve("status", status::class, CfirResolvedDeclarationStatus::class)
    }

    return status
}

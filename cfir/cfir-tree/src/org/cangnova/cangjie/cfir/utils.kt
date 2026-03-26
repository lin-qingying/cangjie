package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
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
import org.cangnova.cangjie.cfir.types.impl.CfirTupleTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirUserTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirVArrayTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.util.wrapIntoFileAnalysisExceptionIfNeeded
import org.cangnova.cangjie.util.wrapIntoSourceCodeAnalysisExceptionIfNeeded

inline fun <R> whileAnalysing(session: CfirSession, element: CfirElement, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        session.exceptionHandler.handleExceptionOnElementAnalysis(element, throwable)
    }
}

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
            source = newSource,
            annotations = typeRef.annotations,
            qualifier = typeRef.qualifier,
            typeArguments = typeRef.typeArguments,
        )

        is CfirFunctionTypeRef -> CfirFunctionTypeRefImpl(
            source = newSource,
            annotations = typeRef.annotations,
            parameterTypeRefs = typeRef.parameterTypeRefs,
            returnTypeRef = typeRef.returnTypeRef,
        )

        is CfirTupleTypeRef -> CfirTupleTypeRefImpl(
            source = newSource,
            annotations = typeRef.annotations,
            elementTypeRefs = typeRef.elementTypeRefs,
        )

        is CfirVArrayTypeRef -> CfirVArrayTypeRefImpl(
            source = newSource,
            annotations = typeRef.annotations,
            elementTypeRef = typeRef.elementTypeRef,
            sizeLiteral = typeRef.sizeLiteral,
        )

        is CfirBasicTypeRef -> CfirBasicTypeRefImpl(
            source = newSource,
            annotations = typeRef.annotations,
            name = typeRef.name,
        )

        is CfirImplicitTypeRef -> CfirImplicitTypeRefImpl(
            annotations = typeRef.annotations,
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

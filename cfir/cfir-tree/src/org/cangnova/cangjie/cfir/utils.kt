package org.cangnova.cangjie.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
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
import org.cangnova.cangjie.cfir.types.CfirParenthesizedTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.impl.CfirQualifierPartImpl
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRefCopy
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRefCopy
import org.cangnova.cangjie.cfir.types.builder.buildParenthesizedTypeRefCopy
import org.cangnova.cangjie.cfir.types.impl.CfirBasicTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirFunctionTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirImplicitTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirOptionTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
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

/**
 * 对位 Kotlin FirDeclarationStatus.copy：签名副本保留原状态的 resolved 类型及全部仓颉状态事实。
 * raw 重建所用的 cloneDeclarationStatus 是另一项合同，不能把已解析声明复制回 raw 状态。
 */
fun CfirDeclarationStatus.copy(
    visibility: Visibility? = this.visibility,
    modality: Modality? = this.modality,
    isVisibilityExplicit: Boolean = this.isVisibilityExplicit,
    isModalityExplicit: Boolean = this.isModalityExplicit,
    isAbstractExplicit: Boolean = this.isAbstractExplicit,
    isOverride: Boolean = this.isOverride,
    isOperator: Boolean = this.isOperator,
    isStatic: Boolean = this.isStatic,
    isConst: Boolean = this.isConst,
    isMut: Boolean = this.isMut,
    isUnsafe: Boolean = this.isUnsafe,
    isForeign: Boolean = this.isForeign,
    isC: Boolean = this.isC,
    isCommon: Boolean = this.isCommon,
    isSpecific: Boolean = this.isSpecific,
    isCommonWithDefault: Boolean = this.isCommonWithDefault,
    isRedef: Boolean = this.isRedef,
    isDefault: Boolean = this.isDefault,
    isAbstract: Boolean = this.isAbstract,
    isOpen: Boolean = this.isOpen,
    isSealed: Boolean = this.isSealed,
): CfirDeclarationStatus {
    val newVisibility = visibility ?: this.visibility
    val newModality = modality ?: this.modality
    return if (this is CfirResolvedDeclarationStatus) {
        buildResolvedDeclarationStatus {
            source = this@copy.source
            this.visibility = newVisibility
            this.modality = checkNotNull(newModality)
            this.isVisibilityExplicit = isVisibilityExplicit
            this.isModalityExplicit = isModalityExplicit
            this.isAbstractExplicit = isAbstractExplicit
            this.isOverride = isOverride
            this.isOperator = isOperator
            this.isStatic = isStatic
            this.isConst = isConst
            this.isMut = isMut
            this.isUnsafe = isUnsafe
            this.isForeign = isForeign
            this.isC = isC
            this.isCommon = isCommon
            this.isSpecific = isSpecific
            this.isCommonWithDefault = isCommonWithDefault
            this.isRedef = isRedef
            this.isDefault = isDefault
            this.isAbstract = isAbstract
            this.isOpen = isOpen
            this.isSealed = isSealed
        }
    } else {
        CfirDeclarationStatusImpl(newVisibility, newModality).also { copied ->
            copied.isVisibilityExplicit = isVisibilityExplicit
            copied.isModalityExplicit = isModalityExplicit
            copied.isAbstractExplicit = isAbstractExplicit
            copied.isOverride = isOverride
            copied.isOperator = isOperator
            copied.isStatic = isStatic
            copied.isConst = isConst
            copied.isMut = isMut
            copied.isUnsafe = isUnsafe
            copied.isForeign = isForeign
            copied.isC = isC
            copied.isCommon = isCommon
            copied.isSpecific = isSpecific
            copied.isCommonWithDefault = isCommonWithDefault
            copied.isRedef = isRedef
            copied.isDefault = isDefault
            copied.isAbstract = isAbstract
            copied.isOpen = isOpen
            copied.isSealed = isSealed
        }
    }
}

/**
 * 在分析 [element] 时运行 [block]，并通过 session 异常处理器包装异常。
 */
inline fun <R> whileAnalysing(session: CfirSession, element: CfirElement, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        session.exceptionHandler.handleExceptionOnElementAnalysis(element, throwable)
    }
}

/**
 * 使用默认 CFIR renderer 渲染元素。
 */
fun CfirElement.render(): String =
   CfirRenderer().renderElementAsString(this)

/**
 * 当前 CFIR 元素对应的 PSI。
 */
val CfirElement.psi: PsiElement? get() = (source as? CjPsiSourceElement)?.psi

/**
 * 在分析 [file] 时运行 [block]，并通过文件级异常处理器包装异常。
 */
inline fun <R> withFileAnalysisExceptionWrapping(file: CfirFile, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        file.moduleData.session.exceptionHandler.handleExceptionOnFileAnalysis(file, throwable)
    }
}

/**
 * CLI 环境使用的 CFIR 异常处理器。
 */
object CfirCliExceptionHandler : CfirExceptionHandler() {
    /**
     * 把元素分析异常包装为源码分析异常后抛出。
     */
    override fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing {
        throw throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element.source)
    }

    /**
     * 把文件分析异常包装为文件分析异常后抛出。
     */
    override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing {
        throw throwable.wrapIntoFileAnalysisExceptionIfNeeded(
            file.sourceFile?.path,
            file.source,
        ) { file.sourceFileLinesMapping?.getLineAndColumnByOffset(it) }
    }
}

/**
 * CFIR 分析异常处理器基类。
 */
abstract class CfirExceptionHandler : CfirSessionComponent {
    /**
     * 处理单个 [element] 分析期间抛出的异常。
     */
    abstract fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing

    /**
     * 处理 [file] 分析期间抛出的异常。
     */
    abstract fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing
}

/**
 * 从 session 中读取 CFIR 异常处理器。
 */
val CfirSession.exceptionHandler: CfirExceptionHandler by CfirSession.sessionComponentAccessor()

/**
 * 复制当前 type ref，并替换 source。
 */
@Suppress("UNCHECKED_CAST")
@OptIn(CfirImplementationDetail::class)
fun <R : CfirTypeRef> R.copyWithNewSource(newSource: CjSourceElement): R {
    if (source == newSource) return this

    return when (val typeRef = this) {
        is CfirErrorTypeRef -> buildErrorTypeRefCopy(typeRef) {
            source = newSource
            partiallyResolvedTypeRef = typeRef.partiallyResolvedTypeRef?.copyWithNewSource(newSource)
        }

        is CfirResolvedTypeRefImpl -> buildResolvedTypeRefCopy(typeRef) {
            source = newSource
        }

        // 未登记的手写resolved concrete不能退化为普通impl；显式拒绝同时穷尽抽象分支。
        is CfirResolvedTypeRef -> error("Resolved type ref has no concrete source-copy contract: " + typeRef::class)

        is org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef -> {
            // 隐式包装没有source槽，公共复制必须遵守同一replaceSource拒绝合同。
            throw IllegalArgumentException("ResolvedImplicitTypeRef cannot acquire a non-null source")
        }

        is CfirParenthesizedTypeRef -> buildParenthesizedTypeRefCopy(typeRef) {
            source = newSource
            innerTypeRef = typeRef.innerTypeRef.copyWithNewSource(newSource)
        }

        is CfirUserTypeRef -> CfirUserTypeRefImpl(
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.typeUseOrigin,
            newSource,
            typeRef.qualifier.map { qualifier ->
                CfirQualifierPartImpl(
                    newSource,
                    qualifier.name,
                    qualifier.typeUseOrigin,
                    qualifier.typeArguments.toMutableOrEmpty(),
                )
            }.toMutableOrEmpty(),
        )

        is CfirFunctionTypeRef -> CfirFunctionTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.typeUseOrigin,
            typeRef.parameterTypeRefs.toMutableList(),
            typeRef.returnTypeRef,
        )

        is CfirOptionTypeRef -> CfirOptionTypeRefImpl(
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.typeUseOrigin,
            newSource,
            typeRef.componentTypeRef,
        )

        is CfirTupleTypeRef -> CfirTupleTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.typeUseOrigin,
            typeRef.elementTypeRefs.toMutableList(),
        )

        is CfirVArrayTypeRef -> CfirVArrayTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.typeUseOrigin,
            typeRef.elementTypeRef,
            typeRef.sizeLiteral,
        )

        is CfirBasicTypeRef -> CfirBasicTypeRefImpl(
            newSource,
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.typeUseOrigin,
            typeRef.name,
        )

        is CfirImplicitTypeRef -> CfirImplicitTypeRefImpl(
            typeRef.annotations.toMutableOrEmpty(),
            typeRef.customRenderer,
            typeRef.typeUseOrigin,
        )

    } as R
}

/**
 * 将 Cone 类型包装成已解析 type ref。
 */
fun ConeCangJieType.toCfirResolvedTypeRef(
    source: CjSourceElement? = null,
    delegatedTypeRef: CfirTypeRef? = null,
): CfirResolvedTypeRef {
    return when (this) {
        is ConeErrorType -> buildErrorTypeRef {
            this.source = source
            this.typeUseOrigin = delegatedTypeRef?.typeUseOrigin ?: org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC
            this.diagnostic = this@toCfirResolvedTypeRef.diagnostic
            this.coneType = this@toCfirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }

        else -> buildResolvedTypeRef {
            this.source = source
            this.typeUseOrigin = delegatedTypeRef?.typeUseOrigin ?: org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC
            this.coneType = this@toCfirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }
    }
}

/**
 * 当前 CFIR 元素对应的真实 PSI。
 */
val CfirElement.realPsi: PsiElement? get() = (source as? CjRealPsiSourceElement)?.psi


/**
 * 在 lazy resolve 中报告实际类型与期望类型不一致的错误。
 */
internal fun CfirBasedSymbol<*>.errorInLazyResolve(name: String, actualClass: KClass<*>, expected: KClass<*>): Nothing {
    errorWithAttachment("Unexpected $name. Expected is ${expected.simpleName}, but was ${actualClass.simpleName}") {
        withCfirEntry("cfirElement", cfir)
        withCfirSymbolIdEntry("cfirSymbol", this@errorInLazyResolve)
    }
}


/**
 * 强制 lazy resolve 到 STATUS 阶段并返回已解析声明状态。
 */
internal fun CfirMemberDeclaration.resolvedStatus(): CfirResolvedDeclarationStatus {
    lazyResolveToPhase(CfirResolvePhase.STATUS)

    val status = status
    if (status !is CfirResolvedDeclarationStatus) {
        symbol.errorInLazyResolve("status", status::class, CfirResolvedDeclarationStatus::class)
    }

    return status
}

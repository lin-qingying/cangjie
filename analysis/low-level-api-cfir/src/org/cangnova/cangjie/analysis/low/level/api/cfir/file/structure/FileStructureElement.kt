

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.body
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialAnalyzable
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialBodyResolvable
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.*

/**
 * Collects [KT -> CFIR][CjToCfirMapping] mapping and [diagnostics][FileStructureElementDiagnostics] for [declaration].
 *
 * @param declaration is a fully resolved declaration (not necessary in [CfirResolvePhase.BODY_RESOLVE] phase)
 *
 * @see FileStructure
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.LLCfirStructureElementDiagnosticsCollector
 */
internal sealed class FileStructureElement(
    val declaration: CfirDeclaration,
    val diagnostics: FileStructureElementDiagnostics,
    elementMapper: LLElementMapper = LLEagerElementMapper(declaration)
) {
    val mappings: CjToCfirMapping = CjToCfirMapping(elementMapper)

    companion object {
        fun recorderFor(fir: CfirDeclaration): CfirElementsRecorder = when (fir) {
            is CfirFile -> RootStructureElement.Recorder(fir)
            is CfirClassLikeDeclaration -> ClassDeclarationStructureElement.Recorder(fir)
            is CfirExtend -> ExtendDeclarationStructureElement.Recorder(fir)
            else -> DeclarationStructureElement.Recorder
        }
    }
}

internal class CjToCfirMapping(private val elementMapper: LLElementMapper) {
    fun get(element: CjElement): CfirElement? {
        return elementMapper(element)
    }

    companion object {
        fun getCfir(
            element: CjElement,
            @Suppress("UNUSED_PARAMETER") session: CfirSession,
            mapping: Map<CjElement, CfirElement>,
        ): CfirElement? {
            var current: PsiElement? = element
            while (
                current == element ||
                current is CjUserType ||
                current is CjTypeReference ||
                current is CjDotQualifiedExpression ||
                current is CjOptionType
            ) {
                // We are still referring to the same element with possible type parameter/name qualification/nullability,
                // hence it is always correct to return a corresponding element if present
                if (current is CjElement) mapping[current]?.let { return it }
                current = current.parent
            }

            // Here current is the lowest ancestor that has different corresponding text
            return when (current) {
                // Constants with unary operation (i.e., +1 or -1) are saved as a leaf element of CFIR tree
                is CjPrefixExpression,
                    // There is no separate element for annotation construction call
                is CjAnnotation,
                    // We replace a source for selector with the whole expression
                is CjSafeQualifiedExpression,
                    // There is no separate CFIR node for this in this@foo expressions, same for super@Foo
                is CjThisExpression,
                is CjSuperExpression,
                    // Part of the path in import/package directives has no CFIR node
                is CjImportDirective,
                is CjPackageDirective,
                    // Super type refs are not recorded
                is CjSuperTypeCallEntry,
                    -> mapping[current as CjElement]
                is CjCallExpression -> {
                    // Case 1:
                    // If we have, say, A(), reference A is not recorded, while call A() is recorded.
                    //
                    // Case 2:
                    // A<Ty> and B<Ty> in `A<Ty>.B<Ty>` are both calls, but neither A nor B nor B<Ty> are recorded.
                    // Only A<Ty> and the whole qualified expression (as CfirResolvedQualifier) are recorded.
                    val parent = current.parent
                    if (current.valueArgumentList == null &&
                        parent is CjQualifiedExpression &&
                        parent.selectorExpression == current
                    ) {
                        mapping[parent]
                    } else {
                        mapping[current]
                    }
                }
                is CjParenthesizedExpression -> null
                // Here there is no separate CFIR node for partial operator calls (like for a[i] = 1, there is no separate node for a[i])
                is CjBinaryExpression -> if (element is CjArrayAccessExpression || element is CjOperationReferenceExpression) {
                    mapping[current]
                } else {
                    null
                }
                is CjBlockExpression -> null
                is PsiErrorElement -> null
                // Value argument names and corresponding references are not part of the CFIR tree
                is CjValueArgumentName -> mapping[current.parent as CjValueArgument]
                is CjContainerNode -> {
                    val parent = current.parent
                    // Labels in labeled expression (i.e., return@foo) have no CFIR node
                    if (parent is CjExpressionWithLabel) mapping[parent] else null
                }
                // Enum entries/annotation entries constructor calls
                is CjConstructorCalleeExpression -> mapping[current.parent as CjCallElement]
                // CjParameter for destructuring declaration
                is CjParameter -> mapping[current as CjElement]
                else -> null
            }
        }
    }
}

internal class ClassDeclarationStructureElement(
    file: CfirFile,
    clazz: CfirClassLikeDeclaration,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElement(
    declaration = clazz,
    diagnostics = FileStructureElementDiagnostics(
        ClassDiagnosticRetriever(
            declaration = clazz,
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
) {
    class Recorder(firClass: CfirClassLikeDeclaration) : CfirElementContainerRecorder(
        container = firClass,
        declarationsToIgnore = firClass.declarationsToIgnore,
    )
}

internal class ExtendDeclarationStructureElement(
    file: CfirFile,
    extend: CfirExtend,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElement(
    declaration = extend,
    diagnostics = FileStructureElementDiagnostics(
        SingleNonLocalDeclarationDiagnosticRetriever(
            declaration = extend,
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
) {
    class Recorder(firExtend: CfirExtend) : CfirElementContainerRecorder(
        container = firExtend,
        declarationsToIgnore = firExtend.declarationsToIgnore,
    )
}

/** @see ClassDeclarationStructureElement */
internal val CfirClassLikeDeclaration.declarationsToIgnore: Set<CfirDeclaration>
    get() = declarations.filterNot(CfirDeclaration::isPartOfClassStructureElement).toSet()

internal val CfirExtend.declarationsToIgnore: Set<CfirDeclaration>
    get() = emptySet()

/**
 * The recorder is supposed to visit only elements that belong to the [container].
 *
 * For instance, it should visit annotations, but not regular declarations.
 */
internal abstract class CfirElementContainerRecorder(
    private val container: CfirDeclaration,
    private val declarationsToIgnore: Set<CfirDeclaration>,
) : CfirElementsRecorder() {
    override fun visitElement(element: CfirElement, data: MutableMap<CjElement, CfirElement>) {
        // Entry point to the visitor
        if (element === container) {
            return super.visitElement(element, data)
        }

        val recordElement = if (element is CfirDeclaration) {
            element !in declarationsToIgnore
        } else {
            true
        }

        if (recordElement) {
            // A separate recorder is called here as we don't have to check
            // conditions for nested elements – they should be recorded deeply
            element.accept(DeclarationStructureElement.Recorder, data)
        }
    }
}

/**
 * Whether a class member declaration is a part of the [ClassDeclarationStructureElement].
 *
 * [CfirClassLikeDeclaration] 作为 class-like 结构单元的锚点；
 * regular class 产生的 synthetic 声明（如 implicit constructor）也归并在此结构单元中。
 * This is necessary to process diagnostics from such elements as they don't have real sources
 * (and a dedicated [FileStructureElement] as a consequence).
 *
 * @see ClassDeclarationStructureElement
 * @see ClassDiagnosticRetriever
 */
internal val CfirDeclaration.isPartOfClassStructureElement: Boolean
    get() = when (source?.kind) {
        CjFakeSourceElementKind.ImplicitConstructor,
        CjFakeSourceElementKind.ClassDelegationField,
            -> true

        else -> false
    }

internal class DeclarationStructureElement(
    file: CfirFile,
    declaration: CfirDeclaration,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElement(
    declaration = declaration,
    diagnostics = FileStructureElementDiagnostics(
        SingleNonLocalDeclarationDiagnosticRetriever(
            declaration = declaration,
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
    elementMapper = createMapper(declaration),
) {
    private companion object {
        private val IS_PARTIAL_RESOLVE_ENABLED by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Registry.`is`("kotlin.analysis.partialBodyAnalysis", true)
        }

        private fun createMapper(declaration: CfirDeclaration): LLElementMapper {
            val partialBodyMapper = createPartialBodyMapperIfApplicable(declaration)
            if (partialBodyMapper != null) {
                return partialBodyMapper
            }

            declaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            return LLEagerElementMapper(declaration)
        }

        private fun createPartialBodyMapperIfApplicable(declaration: CfirDeclaration): LLElementMapper? {
            if (!IS_PARTIAL_RESOLVE_ENABLED) {
                return null
            }

            val bodyBlock = declaration.body
            if (!declaration.isPartialBodyResolvable || bodyBlock == null || declaration.resolvePhase >= CfirResolvePhase.BODY_RESOLVE) {
                return null
            }

            require(declaration.resolvePhase >= CfirResolvePhase.BODY_RESOLVE.previous)

            // 仓颉主干当前没有 Kotlin FIR 的 empty/single-expression block 声明形态，
            // partial body 分支只保留主干真实存在的 lazy block 与普通 block。
            val isPartiallyResolvable = when (bodyBlock) {
                is CfirLazyBlock -> true // Optimistic (however, below we also check the PSI statement count)
                else -> bodyBlock.isPartialAnalyzable
            }

            if (!isPartiallyResolvable) {
                return null
            }

            val session = declaration.llCfirResolvableSession ?: return null
            val psiDeclaration = declaration.realPsi as? CjDeclaration
            val psiBodyBlock = psiDeclaration?.bodyBlock
            val psiStatements = psiBodyBlock?.statements?.takeIf { it.size > 1 } ?: return null

            // Although we don't require the body to be resolved here, its changes must invalidate the element mapper.
            // Note that there might be changes in a number of statements, so here we keep the guarantee – a partial element mapper
            // is only created if there are more than one body statement.
            LLCfirDeclarationModificationService.bodyResolved(declaration, phase = CfirResolvePhase.BODY_RESOLVE)

            return LLPartialBodyElementMapper(declaration, psiDeclaration, psiBodyBlock, psiStatements, session)
        }
    }

    object Recorder : AbstractRecorder()

    /**
     * A recorder that skips content analyzed on the [CfirResolvePhase.BODY_RESOLVE] phase.
     *
     * Sic! The recorder currently is only intended to be used for computing signature mappings in [LLPartialBodyElementMapper]
     * for [isPartialBodyResolvable] declarations.
     * For other usages, the behavior is unspecified.
     */
    class SignatureRecorder(private val declaration: CfirDeclaration) : AbstractRecorder() {
        private var parent: CfirElement? = null

        // Sic! The declaration might be resolved to 'BODY_RESOLVE' in some other thread while we traverse over it.
        override fun visitElement(element: CfirElement, data: MutableMap<CjElement, CfirElement>) {
            // Skip elements only directly nested in the declaration.
            // Note that annotation values technically can contain arbitrary code that we don't want to filter out here.
            val currentParent = parent

            if (element is CfirBlock && currentParent == declaration) {
                // Skip declaration body
                return
            }

            if (element is CfirExpression && currentParent is CfirValueParameter && currentParent.defaultValue == element) {
                // Skip default value parameters
                return
            }

            if (element is CfirFunctionCall && element.origin.isConstructorDelegation && currentParent is CfirConstructor && currentParent == declaration) {
                // Skip delegated constructors
                return
            }

            cacheElement(element, data)

            try {
                parent = element
                element.acceptChildren(this, data)
            } finally {
                parent = currentParent
            }
        }
    }

    class BodyBlockRecorder(block: CfirBlock) : AbstractRecorder() {
        private val statements = block.statements.toSet()

        override fun visitElement(element: CfirElement, data: MutableMap<CjElement, CfirElement>) {
            // Statements are already registered
            if (element !in statements) {
                super.visitElement(element, data)
            }
        }
    }

    abstract class AbstractRecorder : CfirElementsRecorder() {
        override fun visitConstructor(constructor: CfirConstructor, data: MutableMap<CjElement, CfirElement>) {
            super.visitConstructor(constructor, data)

            if (constructor.isPrimary) {
                constructor.valueParameters.forEach { parameter ->
                    parameter.correspondingProperty?.let { property ->
                        visitProperty(property, data)
                    }
                }
            }
        }
    }
}

internal class RootStructureElement(
    file: CfirFile,
    moduleComponents: LLCfirModuleResolveComponents,
) : FileStructureElement(
    declaration = file,
    diagnostics = FileStructureElementDiagnostics(
        FileDiagnosticRetriever(
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
) {
    class Recorder(file: CfirFile) : CfirElementContainerRecorder(
        container = file,
        declarationsToIgnore = file.declarationsToIgnore,
    )
}

/** @see RootStructureElement */
internal val CfirFile.declarationsToIgnore: Set<CfirDeclaration>
    get() = declarations.toSet()

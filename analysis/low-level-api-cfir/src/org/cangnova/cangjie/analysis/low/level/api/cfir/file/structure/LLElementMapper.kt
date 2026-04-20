/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyResolveRequest
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.resolve
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirResolveDesignationCollector
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.body
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.utils.exceptions.buildErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import java.lang.Exception

/**
 * A provider of mapping between [CjElement]s and [CfirElement]s.
 */
internal fun interface LLElementMapper : (CjElement) -> CfirElement?

/**
 * A provider that collects mapping for the [declaration] right on the provider creation.
 * The [declaration] must be fully analyzed up to the [CfirResolvePhase.BODY_RESOLVE].
 */
internal class LLEagerElementMapper(declaration: CfirDeclaration) : LLElementMapper {
    private val session = declaration.moduleData.session

    private val mapping = CfirElementsRecorder.recordElementsFrom(
        cfirElement = declaration,
        recorder = FileStructureElement.recorderFor(declaration),
    )

    override fun invoke(element: CjElement): CfirElement? {
        return CjToCfirMapping.getCfir(element, session, mapping)
    }
}

/**
 * A provider for a partially analyzable [declaration].
 * The declaration must be fully analyzed up to the [CfirResolvePhase.IMPLICIT_TYPES].
 *
 * @param declaration The declaration to be resolved. Must be [isPartiallyAnalyzable].
 * @param psiDeclaration The PSI version of the [declaration].
 * @param psiBlock The block body of the [psiDeclaration].
 * @param psiStatements All statements from the [psiBlock].
 * @param session The session hosting the [declaration].
 */
internal class LLPartialBodyElementMapper(
    private val declaration: CfirDeclaration,
    private val psiDeclaration: CjDeclaration,
    private val psiBlock: CjBlockExpression,
    private val psiStatements: List<CjExpression>,
    private val session: LLCfirResolvableModuleSession
) : LLElementMapper {
    init {
        val phase = declaration.resolvePhase
        checkWithAttachment(
            phase >= CfirResolvePhase.IMPLICIT_TYPES,
            { "The declaration must be at least resolved up to ${CfirResolvePhase.IMPLICIT_TYPES.name}, but it is resolved to $phase" },
        )
    }

    companion object {
        private val LOG = logger<LLPartialBodyElementMapper>()

        private fun createEmptyState(psiStatementCount: Int): LLPartialBodyAnalysisState {
            return LLPartialBodyAnalysisState(
                totalPsiStatementCount = psiStatementCount,
                analyzedPsiStatementCount = 0,
                analyzedCfirStatementCount = 0,
                performedAnalysesCount = 0,
                analysisStateSnapshot = null
            )
        }

        /**
         * Checks whether the [declaration] can be analyzed partially to get the [element] resolved.
         * The [element] must belong to a [declaration].
         */
        fun isPartiallyAnalyzable(element: CjElement, declaration: CjDeclaration): Boolean {
            val block = declaration.bodyBlock ?: return false
            val container = findContainer(element, declaration, block, block.statements)
            return when (container) {
                is ElementContainer.Body, ElementContainer.SignatureBody -> true
                else -> false
            }
        }

        private fun findContainer(
            psiElement: CjElement,
            psiDeclaration: CjDeclaration,
            psiBlock: CjBlockExpression,
            psiStatements: List<CjExpression>,
        ): ElementContainer {
            var previous: PsiElement? = null

            for (current in psiElement.parentsWithSelf) {
                when (current) {
                    psiBlock -> {
                        when (previous) {
                            null -> {
                                // The body block itself is requested.
                                // Here we treat it as a last statement of that block.
                                return ElementContainer.Body(psiStatements.lastIndex)
                            }
                            is CjElement -> {
                                val psiStatementIndex = psiStatements.indexOf(previous)
                                checkWithAttachment(psiStatementIndex >= 0, { "The topmost statement was not found" }) {
                                    withPsiEntry("statement", previous)
                                    withPsiEntry("declaration", psiDeclaration)
                                    withEntry("statements") {
                                        for ((index, psiStatement) in psiStatements.withIndex()) {
                                            append(index)
                                            append(": ")
                                            appendLine(psiStatement.text)
                                        }
                                    }
                                }
                                return ElementContainer.Body(psiStatementIndex)
                            }
                            else -> break
                        }
                    }
                    is CjParameter -> {
                        val parentDeclaration = current.ownerFunction
                        if (parentDeclaration == psiDeclaration) {
                            return if (previous is CjExpression && current.defaultValue == previous) {
                                ElementContainer.SignatureBody
                            } else {
                                ElementContainer.Signature
                            }
                        }
                    }
                    psiDeclaration -> {
                        return ElementContainer.Signature
                    }
                }

                previous = current
            }

            val error = buildErrorWithAttachment(
                message = "Cannot find the element container",
                cause = null as Throwable?,
                buildAttachment = {
                    withPsiEntry("element", psiElement)
                    withPsiEntry("declaration", psiDeclaration)
                },
            )

            LOG.error(error)

            return ElementContainer.Unknown
        }
    }

    /**
     * Contains the latest known partial body resolution state.
     *
     * Initially, the [cachedState] is empty, even though the declaration itself may already be partially resolved.
     * On querying the mapping (by calling [invoke]), the actual resolved state is synchronized with the [cachedState],
     * and all missing elements are added to [bodyMappings].
     */
    @Volatile
    private var cachedState: LLPartialBodyAnalysisState = createEmptyState(psiStatements.size)

    /**
     * Contains mappings for non-body elements.
     */
    private val signatureMappings: Map<CjElement, CfirElement> = HashMap<CjElement, CfirElement>()
        .also { declaration.accept(DeclarationStructureElement.SignatureRecorder(declaration), it) }

    /**
     * Contains collected mappings.
     * Initially, only signature mappings are registered (the body is ignored).
     * After consequent partial body analysis, elements from analyzed statements are appended.
     */
    @Volatile
    private var bodyMappings: Map<CjElement, CfirElement> = emptyMap()

    // The body block cannot be cached on the element provider construction, as the body might be lazy at that point
    private val bodyBlock: CfirBlock
        get() = declaration.body ?: errorWithCfirSpecificEntries(
            "Partial body element provider supports only declarations with bodies",
            fir = declaration,
            psi = psiDeclaration,
        )

    private val lock = Any()

    override fun invoke(psiElement: CjElement): CfirElement? {
        val container = try {
            findContainer(psiElement, psiDeclaration, psiBlock, psiStatements)
        } catch (e: Exception) {
            rethrowExceptionWithDetails("Unable to find the element container", e) {
                withEntry("session", session) { it.toString() }
                withCfirEntry("fir", declaration)
                withPsiEntry("psiElement", psiElement)
            }
        }

        when (container) {
            ElementContainer.Unknown -> return null
            ElementContainer.Signature -> return CjToCfirMapping.getCfir(psiElement, session, signatureMappings)
            ElementContainer.SignatureBody -> {
                // Fast track: the signature body is already analyzed.
                // Synchronization is not needed here as 'cachedState'/'bodyMappings' are addition-only
                if (cachedState.performedAnalysesCount > 0) {
                    // We performed at least one partial analysis, so we definitely analyzed the signature
                    return CjToCfirMapping.getCfir(psiElement, session, bodyMappings)
                }

                // We do not need to analyze any statements.
                // However, parameter analysis is performed before body analysis
                performBodyAnalysis(psiStatementLimit = 0)
            }
            is ElementContainer.Body -> {
                val psiStatementLimit = container.psiStatementIndex + 1

                // Fast track: required statements are already analyzed.
                // Synchronization is not needed here as 'cachedState'/'bodyMappings' are addition-only
                val cachedState = this.cachedState
                if (cachedState.performedAnalysesCount > 0 && cachedState.analyzedPsiStatementCount >= psiStatementLimit) {
                    // The statement is already analyzed and its children are registered
                    return CjToCfirMapping.getCfir(psiElement, session, bodyMappings)
                }

                performBodyAnalysis(psiStatementLimit)
            }
        }

        val bodyMappings = synchronized(lock) {
            // Process newly analyzed statements serially
            processBodyAnalysisResult()
        }

        return CjToCfirMapping.getCfir(psiElement, session, bodyMappings)
    }

    private fun processBodyAnalysisResult(): Map<CjElement, CfirElement> {
        val existingBodyMappings = this.bodyMappings
        val newState = declaration.partialBodyAnalysisState

        if (newState != null) {
            // Pretend we never analyzed the function if the last state is invalid.
            // In this case, all statements starting from the first one will be re-added to the map.
            val cachedState = this.cachedState

            val lastStatementCount = cachedState.analyzedCfirStatementCount
            val newStatementCount = newState.analyzedCfirStatementCount

            val shouldRegisterBodyStatements = newStatementCount > lastStatementCount
            val shouldRegisterSignatureParts = cachedState.performedAnalysesCount == 0

            if (shouldRegisterBodyStatements || shouldRegisterSignatureParts) {
                val newBodyMappings = if (cachedState.performedAnalysesCount > 0) HashMap(existingBodyMappings) else HashMap()

                if (shouldRegisterSignatureParts) {
                    registerSignatureBodyParts(newState, newBodyMappings)
                }

                if (shouldRegisterBodyStatements) {
                    val statements = newState.analysisStateSnapshot?.result?.statements ?: bodyBlock.statements

                    for (index in lastStatementCount until newStatementCount) {
                        val statement = statements[index]
                        statement.accept(DeclarationStructureElement.Recorder, newBodyMappings)
                    }

                    // We can register the block element itself if all its content is analyzed
                    if (newState.isFullyAnalyzed) {
                        val bodyBlock = this.bodyBlock
                        bodyBlock.accept(DeclarationStructureElement.BodyBlockRecorder(bodyBlock), newBodyMappings)
                    }
                }

                // Publish new state
                this.bodyMappings = newBodyMappings
                this.cachedState = newState

                return newBodyMappings
            }
        } else {
            // Another thread might have already produced body mappings
            if (existingBodyMappings.isEmpty()) {
                // The body has never been analyzed (otherwise the partial body resolve state should have been present)
                val newBodyMappings = HashMap<CjElement, CfirElement>()
                    .also { consumer ->
                        bodyBlock.accept(DeclarationStructureElement.Recorder, consumer)
                        registerSignatureBodyParts(newState = null, consumer)
                    }

                this.bodyMappings = newBodyMappings
                return newBodyMappings
            }
        }

        return existingBodyMappings
    }

    private fun registerSignatureBodyParts(newState: LLPartialBodyAnalysisState?, consumer: MutableMap<CjElement, CfirElement>) {
        registerDefaultParameterValues(newState, consumer)
    }

    private fun registerDefaultParameterValues(newState: LLPartialBodyAnalysisState?, consumer: MutableMap<CjElement, CfirElement>) {
        val snapshot = newState?.analysisStateSnapshot
        if (snapshot != null) {
            for (defaultValue in snapshot.result.defaultParameterValues) {
                defaultValue.accept(DeclarationStructureElement.Recorder, consumer)
            }
            return
        }

        if (declaration is CfirFunction) {
            for (parameter in declaration.valueParameters) {
                parameter.defaultValue?.accept(DeclarationStructureElement.Recorder, consumer)
            }
        }
    }

    /**
     * Represents the location of a [PsiElement] for which the CFIR mapping was requested.
     */
    sealed class ElementContainer {
        /**
         * The element resides in a declaration signature analysis of which is already complete.
         * [CfirResolvePhase.BODY_RESOLVE] is not required to get its mapping.
         */
        data object Signature : ElementContainer()

        /**
         * The element is in parts of the signature that require [CfirResolvePhase.BODY_RESOLVE].
         * Example: default parameter values.
         */
        data object SignatureBody : ElementContainer()

        /**
         * The element is inside the declaration body block.
         * [psiStatementIndex] is the index of a topmost block statement which contains the element.
         */
        data class Body(val psiStatementIndex: Int) : ElementContainer()

        /**
         * Some unexpected element.
         */
        data object Unknown : ElementContainer()
    }

    /**
     * Performs partial body analysis up to the [psiStatementLimit] statements.
     * If [psiStatementLimit] is 1, only the first statement is analyzed.
     * If [psiStatementLimit] is 0, statements are not analyzed (but default parameter values are still analyzed).
     */
    private fun performBodyAnalysis(psiStatementLimit: Int) {
        require(psiStatementLimit >= 0)

        if (psiStatementLimit < psiStatements.size) {
            val request = LLPartialBodyResolveRequest(
                target = declaration,
                totalPsiStatementCount = psiStatements.size,
                targetPsiStatementCount = psiStatementLimit,
                stopElement = psiStatements[psiStatementLimit]
            )

            val target = LLCfirResolveDesignationCollector.getDesignationToResolveForPartialBody(request)
            if (target != null) {
                target.resolve(CfirResolvePhase.BODY_RESOLVE)
                return
            }
        }

        declaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
    }
}

/**
 * A [CjBlockExpression] body for a callable declaration.
 */
internal val CjDeclaration.bodyBlock: CjBlockExpression?
    get() = when (this) {
        is CjDeclarationWithBody -> bodyBlockExpression
        else -> null
    }

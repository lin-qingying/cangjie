/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.DuplicatedCfirSourceElementsException
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isErrorElement
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.builder.toCompoundAssignName
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjPsiSourceElement

internal open class CfirElementsRecorder : CfirVisitor<Unit, MutableMap<CjElement, CfirElement>>() {

    /**
     * Note: generally, each CFIR element with a `CjRealPsiSourceElement` source should be mapped to a unique PSI element.
     * If multiple CFIR elements have the same real source PSI element, it is probably a bug in Raw CFIR building code.
     */
    private fun cache(psi: CjElement, fir: CfirElement, cache: MutableMap<CjElement, CfirElement>) {
        val existingCfir = cache[psi]
        if (existingCfir != null && existingCfir !== fir) {
            when {
                existingCfir is CfirTypeRef && fir is CfirTypeRef && psi is CjTypeReference -> {
                    // CfirTypeRefs are often created during resolve
                    // a lot of them with have the same source
                    // we want to take the most "resolved one" here
                    if (fir is CfirResolvedTypeRefImpl && existingCfir !is CfirResolvedTypeRefImpl) {
                        cache[psi] = fir
                    }
                }
                existingCfir.isErrorElement && !fir.isErrorElement -> {
                    // TODO better handle error elements
                    // but for now just take first non-error one if such exist
                    cache[psi] = fir
                }
                existingCfir.isErrorElement || fir.isErrorElement -> {
                    // do nothing and maybe upgrade to a non-error element in the branch above in the future
                }
                else -> {
                    if (DuplicatedCfirSourceElementsException.IS_ENABLED) {
                        throw DuplicatedCfirSourceElementsException(existingCfir, fir, psi)
                    }
                }
            }
        }
        if (existingCfir == null) {
            cache[psi] = fir
        }
    }

    override fun visitElement(element: CfirElement, data: MutableMap<CjElement, CfirElement>) {
        cacheElement(element, data)
        element.acceptChildren(this, data)
    }

    override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: MutableMap<CjElement, CfirElement>) {
        for (bound in typeParameter.bounds) {
            val boundPsi = (bound.source as? CjPsiSourceElement)?.psi
            val constraintSubject = (boundPsi?.parent as? CjTypeConstraint)?.subjectTypeParameterName ?: continue
            cache(constraintSubject, typeParameter, data)
        }
        super.visitTypeParameter(typeParameter, data)
    }

    override fun visitAssignment(assignment: CfirAssignment, data: MutableMap<CjElement, CfirElement>) {
        // 对标 Kotlin 的 write-mapping 语义：赋值左侧命中赋值节点本身。
        val lValuePsi = (assignment.lValue.source as? CjPsiSourceElement)?.psi as? CjElement
        lValuePsi?.let { cache(it, assignment, data) }
        visitElement(assignment, data)
    }

    override fun visitLiteralExpression(literalExpression: CfirLiteralExpression, data: MutableMap<CjElement, CfirElement>) {
        cacheElement(literalExpression, data)
        literalExpression.annotations.forEach {
            it.accept(this, data)
        }
        // CjPrefixExpression(-, CjConstExpression(n)) is represented as CfirLiteralExpression(-n) with converted constant value.
        // If one queries CFIR for CjConstExpression, we still return CfirLiteralExpression(-n) even though its source is CjPrefixExpression.
        // Here, we cache CfirLiteralExpression(n) for CjConstExpression(n) to make everything natural and intuitive!
        if (literalExpression.isConverted) {
            literalExpression.kind.reverseConverted(literalExpression)?.let { cacheElement(it, data) }
        }
    }

    override fun visitBindingPattern(bindingPattern: CfirBindingPattern, data: MutableMap<CjElement, CfirElement>) {
        recordPatternBindingVariable(bindingPattern, bindingPattern.bindingVariable, data)
        bindingPattern.typeRef?.accept(this, data)
        bindingPattern.bindingVariable?.accept(this, data)
        bindingPattern.nestedPattern?.accept(this, data)
    }

    override fun visitVarOrEnumPattern(varOrEnumPattern: CfirVarOrEnumPattern, data: MutableMap<CjElement, CfirElement>) {
        recordPatternBindingVariable(varOrEnumPattern, varOrEnumPattern.bindingVariable, data)
        varOrEnumPattern.bindingVariable?.accept(this, data)
    }

    override fun visitTypePattern(typePattern: CfirTypePattern, data: MutableMap<CjElement, CfirElement>) {
        recordPatternBindingVariable(typePattern, typePattern.bindingVariable, data)
        typePattern.typeRef.accept(this, data)
        typePattern.bindingVariable?.accept(this, data)
    }

    //@formatter:off
    override fun visitReference(reference: CfirReference, data: MutableMap<CjElement, CfirElement>) {}
    override fun visitControlFlowGraphReference(controlFlowGraphReference: CfirControlFlowGraphReference, data: MutableMap<CjElement, CfirElement>) {}
    override fun visitNamedReference(namedReference: CfirNamedReference, data: MutableMap<CjElement, CfirElement>) {}
    override fun visitThisReference(thisReference: CfirThisReference, data: MutableMap<CjElement, CfirElement>) {}
    //@formatter:on

    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: MutableMap<CjElement, CfirElement>) {
        super.visitResolvedTypeRef(errorTypeRef, data)
        recordTypeQualifiers(errorTypeRef, data)
        errorTypeRef.delegatedTypeRef?.accept(this, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: MutableMap<CjElement, CfirElement>) {
        super.visitResolvedTypeRef(resolvedTypeRef, data)
        recordTypeQualifiers(resolvedTypeRef, data)
        resolvedTypeRef.delegatedTypeRef?.accept(this, data)
    }

    override fun visitUserTypeRef(userTypeRef: CfirUserTypeRef, data: MutableMap<CjElement, CfirElement>) {
        userTypeRef.acceptChildren(this, data)
    }

    override fun visitOptionTypeRef(optionTypeRef: CfirOptionTypeRef, data: MutableMap<CjElement, CfirElement>) {
        optionTypeRef.acceptChildren(this, data)
    }

    protected fun cacheElement(element: CfirElement, cache: MutableMap<CjElement, CfirElement>) {
        val psi = element.anchorPsi as? CjElement ?: return
        cache(psi, element, cache)
    }

    /**
     * 仓颉 pattern 的源码节点对应语义绑定声明，而不是 pattern 容器本身。
     *
     * 对齐官方编译器 `VarPattern.varDecl`：pattern 负责语法结构，
     * 进入作用域并暴露给 Analysis API 的是内部绑定变量声明。
     */
    private fun recordPatternBindingVariable(
        pattern: CfirElement,
        bindingVariable: CfirPatternBindingVariable?,
        cache: MutableMap<CjElement, CfirElement>,
    ) {
        val psi = pattern.anchorPsi as? CjElement ?: return
        val variable = bindingVariable ?: return
        cache(psi, variable, cache)
    }

    private val CfirLiteralExpression.isConverted: Boolean
        get() {
            val cfirSourcePsi = this.source?.psi ?: return false
            return cfirSourcePsi is CjPrefixExpression && cfirSourcePsi.operationToken == CjTokens.MINUS
        }

    private val CfirLiteralExpression.constantExpression: CjConstantExpression?
        get() {
            val cfirSourcePsi = this.source?.psi
            return cfirSourcePsi?.findDescendantOfType()
        }

    private fun CfirLiteralKind.reverseConverted(original: CfirLiteralExpression): CfirLiteralExpression? {
        val value = original.value as? Number ?: return null
        val convertedValue: Any = when (this) {
            CfirLiteralKind.INT -> when (value) {
                is Byte -> value.toByte().unaryMinus()
                is Short -> value.toShort().unaryMinus()
                is Int -> value.toInt().unaryMinus()
                is Long -> value.toLong().unaryMinus()
                else -> return null
            }
            CfirLiteralKind.FLOAT -> when (value) {
                is Float -> value.toFloat().unaryMinus()
                is Double -> value.toDouble().unaryMinus()
                else -> return null
            }
            else -> null
        } ?: return null
        return buildLiteralExpression {
            source = original.constantExpression?.toCjPsiSourceElement()
            kind = this@reverseConverted
            this.value = convertedValue
        }.also {
            it.replaceConeTypeOrNull(original.resolvedType)
        }
    }

    private fun recordTypeQualifiers(resolvedTypeRef: CfirResolvedTypeRef, data: MutableMap<CjElement, CfirElement>) {
        val userTypeRef = resolvedTypeRef.delegatedTypeRef as? CfirUserTypeRef ?: return
        val qualifiers = userTypeRef.qualifier
        if (qualifiers.size <= 1) return
        qualifiers.forEach { qualifier ->
            val qualifierPsi = qualifier.anchorPsi as? CjElement ?: return@forEach
            cache(qualifierPsi, resolvedTypeRef, data)
        }
    }

    companion object {
        fun recordElementsFrom(cfirElement: CfirElement, recorder: CfirElementsRecorder): Map<CjElement, CfirElement> =
            buildMap { cfirElement.accept(recorder, this) }

        /**
         * The PSI element which can be used as an anchor point for CFIR <–> PSI mapping.
         *
         * Not all fake CFIR elements might have an anhor PSI element to avoid conflict with the original source element.
         * For instance, the synthetic enum supertype would have the same psi as the class itself, so it shouldn't be used
         * as an anchor to avoid ambiguity. Clients won't expect to see the supertype type reference value instead of the [CfirClass][org.cangnova.cangjie.cfir.declarations.CfirClass]
         * by [CjClass] key.
         */
        val CfirElement.anchorPsi: PsiElement?
            get() {
                val source = source as? CjPsiSourceElement? ?: return null
                when (source.kind) {
                    CjRealSourceElementKind,
                    CjFakeSourceElementKind.ReferenceInAtomicQualifiedAccess,
                    CjFakeSourceElementKind.FromUseSiteTarget,
                        // To allow type retrieval from erroneous typealias even though it is erroneous
                    CjFakeSourceElementKind.ErroneousTypealiasExpansion,
                        // For secondary constructors without explicit delegated constructor call, the PSI tree always create an empty
                        // CjConstructorDelegationCall. In this case, the source in CFIR has this fake source kind.
                    CjFakeSourceElementKind.ImplicitConstructor,
                    CjFakeSourceElementKind.DanglingModifierList,
                        -> Unit

                    else if (
                            source.isSourceForSmartCasts(this) ||
                                    source.isSourceForArrayAugmentedAssign(this) ||
                                    source.isSourceForCompoundAccess(this)
                            )
                        -> Unit

                    else -> return null
                }

                return source.psi
            }

        /**
         * CFIR represents compound assignment and inc/dec operations as multiple smaller instructions. Here we choose the write operation as the
         * resolved CfirElement for binary and unary expressions. For example, the `CfirVariableAssignment` or the call to `set` or `plusAssign`
         * function, etc. This is because the write CfirElement can be used to retrieve all other information related to this compound operation.

         * On the other hand, if the PSI is the left operand of an assignment or the base expression of a unary expression, we take the read CFIR
         * element so the user of the Analysis API is able to retrieve such read calls reliably.
         */
        private fun CjSourceElement.isSourceForCompoundAccess(fir: CfirElement): Boolean {
            val psi = psi
            val parentPsi = psi?.parent
            if (kind !is CjFakeSourceElementKind.DesugaredAugmentedAssign && kind !is CjFakeSourceElementKind.DesugaredIncrementOrDecrement) {
                return false
            }
            return when {
                psi is CjBinaryExpression || psi is CjUnaryExpression -> fir.isWriteInCompoundCall()
                parentPsi is CjBinaryExpression && psi == parentPsi.left -> fir.isReadInCompoundCall()
                parentPsi is CjUnaryExpression && psi == parentPsi.baseExpression -> fir.isReadInCompoundCall()
                else -> false
            }
        }

        // After desugaring, we also have CfirBlock with the same source element.
        // We need to filter it out to map this source element to set/plusAssign call, so we check `is CfirFunctionCall`
        private fun CjSourceElement.isSourceForArrayAugmentedAssign(fir: CfirElement): Boolean {
            return kind is CjFakeSourceElementKind.DesugaredAugmentedAssign && (fir is CfirFunctionCall || fir is CfirThisReceiverExpression)
        }

        // `CfirSmartCastExpression` forward the source from the original expression,
        // and implicit receivers have fake sources pointing to a wider part of the expression.
        // Thus, `CfirElementsRecorder` may try assigning an unnecessarily wide source
        // to smart cast expressions, which will affect the
        // `org.cangnova.cangjie.idea.highlighting.highlighters.ExpressionsSmartcastHighlighter#highlightExpression`
        // function in intellij.git
        private fun CjSourceElement.isSourceForSmartCasts(fir: CfirElement) =
            (kind is CjFakeSourceElementKind.SmartCastExpression) && fir is CfirSmartCastExpression && !fir.originalExpression.isImplicitThisReceiver

        private val CfirExpression.isImplicitThisReceiver get() = this is CfirThisReceiverExpression && this.calleeReference.isImplicit

        private fun CfirElement.isReadInCompoundCall(): Boolean {
            if (this is CfirNamedAccessExpression) return true
            if (this !is CfirFunctionCall) return false
            val name = (calleeReference as? CfirResolvedNamedReference)?.name ?: getFallbackCompoundCalleeName()
            return name == OperatorNameConventions.GET
        }

        private fun CfirElement.isWriteInCompoundCall(): Boolean {
            if (this is CfirAssignment) return true
            if (this !is CfirFunctionCall) return false
            val name = (calleeReference as? CfirResolvedNamedReference)?.name ?: getFallbackCompoundCalleeName()
            return name == OperatorNameConventions.SET || name in OperatorConventions.ASSIGNMENT_OPERATIONS.values
        }

        /**
         * If the callee reference is not a [CfirResolvedNamedReference], we can get the compound callee name from the source instead. For
         * example, if the callee reference is a [CfirErrorNamedReference] with an unresolved name `plusAssign`, the operation element type from
         * the source will be `CjTokens.PLUSEQ`, which can be transformed to `plusAssign`.
         */
        private fun CfirElement.getFallbackCompoundCalleeName(): Name? {
            val psi = source.psi as? CjOperationExpression ?: return null
            val operationReference = psi.operationReference
            return operationReference.getAssignmentOperationName() ?: operationReference.referencedNameAsName
        }

        private fun CjSimpleNameExpression.getAssignmentOperationName(): Name? {
            return referencedNameElementType.toCompoundAssignName()
        }
    }
}

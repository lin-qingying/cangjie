package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef

sealed class ResolutionMode(
    val forceFullCompletion: Boolean,
) {
    open val hintForContextSensitiveResolution: ConeCangJieType? get() = null

    open class ContextDependent(
        override val hintForContextSensitiveResolution: ConeCangJieType?,
    ) : ResolutionMode(forceFullCompletion = false) {
        companion object : ContextDependent(hintForContextSensitiveResolution = null)
    }

    data object ContextIndependent : ResolutionMode(forceFullCompletion = true)

    sealed class ReceiverResolution(val forCallableReference: Boolean) : ResolutionMode(forceFullCompletion = true) {
        data object ForCallableReference : ReceiverResolution(forCallableReference = true)
        companion object : ReceiverResolution(forCallableReference = false)
    }

    class WithExpectedType(
        val expectedTypeRef: CfirResolvedTypeRef,
        val lastStatementInBlock: Boolean = false,
        val fromCast: Boolean = false,
        val arrayLiteralPosition: ArrayLiteralPosition? = null,
        override val hintForContextSensitiveResolution: ConeCangJieType? = null,
        forceFullCompletion: Boolean = true,
    ) : ResolutionMode(forceFullCompletion) {
        val expectedType: ConeCangJieType
            get() = expectedTypeRef.coneType

        fun copy(
            expectedTypeRef: CfirResolvedTypeRef = this.expectedTypeRef,
            lastStatementInBlock: Boolean = this.lastStatementInBlock,
            forceFullCompletion: Boolean = this.forceFullCompletion,
        ): WithExpectedType = WithExpectedType(
            expectedTypeRef = expectedTypeRef,
            lastStatementInBlock = lastStatementInBlock,
            fromCast = fromCast,
            arrayLiteralPosition = arrayLiteralPosition,
            hintForContextSensitiveResolution = hintForContextSensitiveResolution,
            forceFullCompletion = forceFullCompletion,
        )
    }

    enum class ArrayLiteralPosition {
        AnnotationArgument,
        AnnotationParameter,
    }

    class WithStatus(val status: CfirDeclarationStatus) : ResolutionMode(forceFullCompletion = false)

    class UpdateImplicitTypeRef(val newTypeRef: CfirResolvedTypeRef) : ResolutionMode(forceFullCompletion = false)
}

val ResolutionMode.expectedType: ConeCangJieType?
    get() = when (this) {
        is ResolutionMode.WithExpectedType -> expectedType.takeIf { !fromCast }
        else -> null
    }

fun withExpectedType(
    expectedTypeRef: CfirTypeRef,
    arrayLiteralPosition: ResolutionMode.ArrayLiteralPosition? = null,
    hintForContextSensitiveResolution: ConeCangJieType? = null,
): ResolutionMode = when (expectedTypeRef) {
    is CfirResolvedTypeRef -> ResolutionMode.WithExpectedType(
        expectedTypeRef = expectedTypeRef,
        arrayLiteralPosition = arrayLiteralPosition,
        hintForContextSensitiveResolution = hintForContextSensitiveResolution,
    )
    else -> ResolutionMode.ContextIndependent
}

fun withExpectedType(coneType: ConeCangJieType?, lastStatementInBlock: Boolean = false): ResolutionMode {
    return coneType?.let { withExpectedType(it, lastStatementInBlock) } ?: ResolutionMode.ContextDependent
}

fun withExpectedType(coneType: ConeCangJieType, lastStatementInBlock: Boolean = false): ResolutionMode {
    val typeRef = buildResolvedTypeRef {
        this.coneType = coneType
    }
    return ResolutionMode.WithExpectedType(typeRef, lastStatementInBlock)
}

fun CfirDeclarationStatus.mode(): ResolutionMode =
    ResolutionMode.WithStatus(this)

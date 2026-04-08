package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 专门承接“通用 type mismatch 之下还能继续细分的语义”。
 *
 * 这层故意既不直接做 checker，也不直接绑在 cone 映射上，
 * 因为同一条细分规则可能同时被：
 * - declaration / expression checker 直接使用；
 * - coneDiagnostic -> CfirDiagnostic 映射复用。
 *
 * 这样可以避免 VArray 这类类型系统专门语义在多个入口重复实现。
 */
internal sealed interface CfirSpecificTypeMismatch {
    data class VArraySizeMismatch(
        val elementType: ConeCangJieType,
        val expectedSize: Long,
        val actualSize: Long,
    ) : CfirSpecificTypeMismatch
}

internal fun classifySpecificTypeMismatch(
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    session: CfirSession,
): CfirSpecificTypeMismatch? {
    val expandedExpectedType = expectedType.fullyExpandedType(session)
    val expandedActualType = actualType.fullyExpandedType(session)

    val expectedVArray = expandedExpectedType as? ConeVArrayType ?: return null
    val actualVArray = expandedActualType as? ConeVArrayType ?: return null
    if (expectedVArray.size == actualVArray.size) return null

    val sameElementType = AbstractTypeChecker.equalTypes(
        session.typeContext,
        expectedVArray.elementType,
        actualVArray.elementType,
    )
    if (!sameElementType) return null

    return CfirSpecificTypeMismatch.VArraySizeMismatch(
        elementType = expectedVArray.elementType,
        expectedSize = expectedVArray.size,
        actualSize = actualVArray.size,
    )
}

internal fun specificTypeMismatchDiagnostic(
    source: AbstractCjSourceElement?,
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source as? CjSourceElement ?: return null
    return when (val mismatch = classifySpecificTypeMismatch(expectedType, actualType, session)) {
        is CfirSpecificTypeMismatch.VArraySizeMismatch -> createVArraySizeMismatchDiagnostic(
            source = diagnosticSource,
            mismatch = mismatch,
            session = session,
        )

        null -> null
    }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun createVArraySizeMismatchDiagnostic(
    source: CjSourceElement,
    mismatch: CfirSpecificTypeMismatch.VArraySizeMismatch,
    session: CfirSession,
): CjDiagnostic? {
    return CfirErrors.VARRAY_SIZE_MISMATCH.on(
        source,
        mismatch.expectedSize,
        mismatch.actualSize,
        mismatch.elementType,
        null,
        diagnosticContext(session),
    )
}

private fun diagnosticContext(session: CfirSession): DiagnosticContext {
    return object : DiagnosticContext {
        override val languageVersionSettings = session.languageVersionSettings
        override val containingFilePath: String? = null
        override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
    }
}

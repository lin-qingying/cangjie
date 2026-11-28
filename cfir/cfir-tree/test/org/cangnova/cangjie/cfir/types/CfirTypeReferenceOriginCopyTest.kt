@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildParenthesizedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 公共source-copy保留原型/错误concrete身份；无source包装拒绝非法迁移。 */
class CfirTypeReferenceOriginCopyTest {
    @Test
    fun sourceCopyRetainsEveryPublishedOriginAndTheOriginalPrototype() {
        for (origin in CfirTypeRefOrigin.entries) {
            val oldSource = source("old-$origin")
            val newSource = source("new-$origin")
            val prototype = buildResolvedTypeRef { source = oldSource; typeUseOrigin = origin; coneType = ConePrimitiveType.INT32 }
            val original = buildResolvedTypeRef {
                source = oldSource; typeUseOrigin = origin; coneType = ConePrimitiveType.INT32; delegatedTypeRef = prototype
            }
            val copied = original.copyWithNewSource(newSource)
            assertNotSame(original, copied)
            assertEquals(original::class, copied::class)
            assertSame(newSource, copied.source)
            assertSame(oldSource, original.source)
            assertSame(prototype, copied.delegatedTypeRef)
            assertEquals(origin, copied.typeUseOrigin)
        }
    }

    @Test
    fun errorCopyRetainsDiagnosticAndMigratesThePartialRefWithoutLosingItsOrigin() {
        val oldSource = source("old-error")
        val newSource = source("new-error")
        val diagnostic = ConeSimpleDiagnostic("missing final qualifier")
        val partial = buildResolvedTypeRef {
            source = oldSource; typeUseOrigin = CfirTypeRefOrigin.COMPILER_ADDED; coneType = ConePrimitiveType.INT32
        }
        val original = buildErrorTypeRef {
            source = oldSource; typeUseOrigin = CfirTypeRefOrigin.SOURCE
            this.diagnostic = diagnostic; partiallyResolvedTypeRef = partial; delegatedTypeRef = partial
        }
        val copied = original.copyWithNewSource(newSource)
        assertSame(diagnostic, copied.diagnostic)
        assertEquals(original::class, copied::class)
        assertEquals(CfirTypeRefOrigin.SOURCE, copied.typeUseOrigin)
        assertNotSame(partial, copied.partiallyResolvedTypeRef)
        assertSame(newSource, copied.partiallyResolvedTypeRef?.source)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, copied.partiallyResolvedTypeRef?.typeUseOrigin)
        assertSame(partial, copied.delegatedTypeRef)
        assertSame(oldSource, partial.source)
    }

    @Test
    fun resolvedImplicitWrapperRejectsNonNullSourceWithoutChangingTheKnownType() {
        val known = buildResolvedTypeRef { coneType = ConePrimitiveType.INT32 }
        val wrapper = ResolvedImplicitTypeRef(known)
        assertThrows(IllegalArgumentException::class.java) { wrapper.copyWithNewSource(source("illegal-wrapper-source")) }
        assertNull(wrapper.source)
        assertSame(known, wrapper.typeRef)
        assertEquals(CfirTypeRefOrigin.SEMANTIC, wrapper.typeUseOrigin)
    }

    @Test
    fun parenthesizedSourceCopyPreservesTheIndependentInnerOrigin() {
        val originalSource = source("paren-original")
        val targetSource = source("paren-target")
        val inner = buildResolvedTypeRef {
            source = originalSource; typeUseOrigin = CfirTypeRefOrigin.COMPILER_ADDED; coneType = ConePrimitiveType.INT32
        }
        val original = buildParenthesizedTypeRef {
            source = originalSource; typeUseOrigin = CfirTypeRefOrigin.SOURCE; innerTypeRef = inner
        }
        val copied = original.copyWithNewSource(targetSource)
        assertNotSame(original, copied)
        assertNotSame(inner, copied.innerTypeRef)
        assertEquals(CfirTypeRefOrigin.SOURCE, copied.typeUseOrigin)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, copied.innerTypeRef.typeUseOrigin)
        assertSame(originalSource, inner.source)
        assertSame(targetSource, copied.innerTypeRef.source)
    }

    private fun source(identity: String) = object : CjBinarySourceElement(identity, null, identity) {}
}

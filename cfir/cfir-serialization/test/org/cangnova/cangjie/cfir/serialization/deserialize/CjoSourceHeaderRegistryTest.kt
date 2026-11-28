package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.Package
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.Base64

/** 官方已保存非零正文用于证明header读取不会取得正文/局部声明能力，不手造CFIR或语义结果。 */
class CjoSourceHeaderRegistryTest {
    @Test fun headerOfProbeDoesNotClaimLocalVariables() {
        val wire = input("read")
        val headers = CjoSourceOwnershipPlan.headers(wire)
        assertHeaderOnly(headers)
        assertTrue(headers.functions.values.any { it.body.body != 0u })
        assertFalse(headers.declarations.values.any { it.wire.identifier == "x" || it.wire.identifier == "y" })
        val content = CjoSourceOwnershipPlan.create(wire)
        assertEquals(CjoSourcePlanKind.CONTENT, content.kind)
        assertTrue(content.declarations.values.any { it.wire.identifier == "x" })
        assertTrue(content.expressions.isNotEmpty())
        assertThrows(IllegalStateException::class.java) { headers.selectExpressions(listOf(1u)) }
    }

    @Test fun constructorHeadersDoNotClaimNestedFunctionBody() {
        val wire = input("nested_other")
        val headers = CjoSourceOwnershipPlan.headers(wire)
        assertHeaderOnly(headers)
        assertEquals(2, headers.functions.size)
        assertTrue(headers.declarations.values.any { it.wire.identifier == "other" })
        assertFalse(headers.declarations.values.any { it.wire.identifier == "readOther" })
        assertTrue(CjoSourceOwnershipPlan.create(wire).declarations.values.any { it.wire.identifier == "readOther" })
        headers.functions.values.forEach { function ->
            assertTrue(function.lexicalScope.declarationContext is CjoSourceContext.Declaration)
            val owner = (function.lexicalScope.declarationContext as CjoSourceContext.Declaration).reference
            assertEquals("Box", headers.declarations.getValue(owner).wire.identifier)
        }
    }

    @Test fun finalizerHeaderRetainsOriginalFWithoutReadingLocalFunction() {
        val wire = input("finalizer_nested_field")
        val headers = CjoSourceOwnershipPlan.headers(wire)
        assertHeaderOnly(headers)
        val finalizer = headers.declarations.values.single { it.wire.identifier == "~init" }
        val identity = headers.functionByDeclaration.getValue(finalizer.reference)
        val function = headers.functions.getValue(identity)
        assertEquals(finalizer.reference, function.declaration)
        assertTrue(function.body.body != 0u)
        assertFalse(headers.declarations.values.any { it.wire.identifier == "readParent" || it.wire.identifier == "observed" })
        val content = CjoSourceOwnershipPlan.create(wire)
        assertEquals(identity, content.functionByDeclaration.getValue(finalizer.reference))
        assertTrue(content.declarations.values.any { it.wire.identifier == "readParent" })
    }

    @Test fun explicitProbeBodyCarriesBorrowedClassWithoutClassBodies() {
        val headers = CjoSourceOwnershipPlan.headers(input("read"))
        val probe = headers.declarations.values.single { it.wire.identifier == "probe" }
        val identity = headers.functionByDeclaration.getValue(probe.reference)
        val selection = CjoSourceOwnershipPlan.request(headers, listOf(CjoSourceContentRoot.FunctionBody(identity)))
        assertEquals(setOf(identity), selection.selectedBodies)
        assertEquals(setOf(identity), selection.ownedContent.functions.keys)
        assertTrue(selection.ownedContent.declarations.values.any { it.wire.identifier == "x" })
        assertFalse(selection.ownedContent.declarations.values.any { it.wire.identifier == "Cell" })
        assertTrue(selection.borrowedContexts.declarations.values.any { it.wire.identifier == "Cell" })
        assertSame(headers.functions.getValue(identity), selection.borrowedContexts.functions.getValue(identity))
        assertTrue(selection.declarationBindings.isNotEmpty())
    }

    @Test fun nestedConstructorRequestDoesNotSelectSiblingConstructor() {
        val headers = CjoSourceOwnershipPlan.headers(input("nested_other"))
        val requested = headers.functions.values.single { function ->
            val parameter = checkNotNull(function.body.paramLists(0)).params(0)
            headers.declarations.getValue(parameter).wire.identifier == "other"
        }
        val sibling = headers.functions.values.single { it !== requested }
        val selection = CjoSourceOwnershipPlan.request(headers, listOf(CjoSourceContentRoot.FunctionBody(requested.identity)))
        assertEquals(2, selection.selectedBodies.size)
        assertTrue(requested.identity in selection.selectedBodies)
        assertFalse(sibling.identity in selection.selectedBodies)
        assertFalse(sibling.body.body in selection.ownedContent.expressions)
        assertTrue(selection.ownedContent.declarations.values.any { it.wire.identifier == "readOther" })
        assertTrue(selection.borrowedContexts.declarations.values.any { it.wire.identifier == "Box" })
        assertTrue(selection.borrowedContexts.declarations.values.any { it.wire.identifier == "other" })
    }

    @Test fun finalizerRequestCarriesBothOwningAndLexicalRoots() {
        val headers = CjoSourceOwnershipPlan.headers(input("finalizer_nested_field"))
        val finalizer = headers.declarations.values.single { it.wire.identifier == "~init" }
        val identity = headers.functionByDeclaration.getValue(finalizer.reference)
        val selection = CjoSourceOwnershipPlan.request(headers, listOf(CjoSourceContentRoot.FunctionBody(identity)))
        assertEquals(2, selection.selectedBodies.size)
        assertTrue(identity in selection.borrowedContexts.functions)
        assertTrue(selection.borrowedContexts.declarations.values.any { it.wire.identifier == "Child" })
        assertTrue(selection.borrowedContexts.declarations.values.any { it.wire.identifier == "Parent" })
        val nested = selection.ownedContent.declarations.values.single { it.wire.identifier == "readParent" }
        val nestedIdentity = selection.ownedContent.functionByDeclaration.getValue(nested.reference)
        assertEquals(identity, selection.borrowedContexts.functions.getValue(nestedIdentity).lexicalScope.function)
        assertFalse(selection.ownedContent.declarations.containsKey(finalizer.reference))
    }

    @Test fun absentDefaultRetainsHeaderContextWithoutInventingExpression() {
        val headers = CjoSourceOwnershipPlan.headers(input("read"))
        val parameter = headers.declarations.values.single { it.wire.identifier == "a" }
        val selection = CjoSourceOwnershipPlan.request(headers, listOf(CjoSourceContentRoot.ParameterDefault(parameter.reference)))
        assertTrue(selection.ownedContent.expressions.isEmpty())
        assertTrue(selection.selectedBodies.isEmpty())
        assertSame(parameter, selection.borrowedContexts.declarations.getValue(parameter.reference))
        assertTrue(selection.borrowedContexts.functions.isNotEmpty())
    }

    @Test fun discoveredLocalFunctionCanBeRequestedFromNewRegistry() {
        val original = CjoSourceOwnershipPlan.headers(input("nested_other"))
        val constructor = original.functions.values.single { function ->
            original.declarations.getValue(checkNotNull(function.body.paramLists(0)).params(0)).wire.identifier == "other"
        }
        val outer = CjoSourceOwnershipPlan.request(original, listOf(CjoSourceContentRoot.FunctionBody(constructor.identity)))
        val discovered = outer.registeredHeaders
        assertHeaderOnly(discovered)
        assertFalse(original.declarations.values.any { it.wire.identifier == "readOther" })
        val local = discovered.declarations.values.single { it.wire.identifier == "readOther" }
        val identity = discovered.functionByDeclaration.getValue(local.reference)
        val selected = CjoSourceOwnershipPlan.request(discovered, listOf(CjoSourceContentRoot.FunctionBody(identity)))
        assertEquals(setOf(identity), selected.selectedBodies)
        assertFalse(constructor.body.body in selected.ownedContent.expressions)
        assertEquals(constructor.identity, selected.borrowedContexts.functions.getValue(identity).lexicalScope.function)
        assertTrue(discovered.expressionContexts.isNotEmpty())
        assertTrue(discovered.expressions.isEmpty())
    }

    private fun assertHeaderOnly(headers: CjoSourceOwnershipPlan) {
        assertEquals(CjoSourcePlanKind.HEADER_REGISTRY, headers.kind)
        assertTrue(headers.expressions.isEmpty())
        assertTrue(headers.expressionOccurrences.isEmpty())
        assertTrue(headers.declarations.values.none { it.initializerSelected })
        headers.functions.values.forEach { function ->
            assertFalse(function.lexicalScope.includesInitializers)
            assertSame(function, headers.functions.getValue(function.identity))
        }
    }

    private fun input(id: String): Package {
        val text = checkNotNull(javaClass.getResourceAsStream("/sourceHeaderRegistry/$id.cjo.base64")).use {
            it.readAllBytes().toString(Charsets.UTF_8).trim()
        }
        return Package.getRootAsPackage(ByteBuffer.wrap(Base64.getDecoder().decode(text)))
    }
}

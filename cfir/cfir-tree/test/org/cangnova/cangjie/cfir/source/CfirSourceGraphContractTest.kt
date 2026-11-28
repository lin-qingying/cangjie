package org.cangnova.cangjie.cfir.source

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationArgumentMappingImpl
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.IdentityHashMap

/** 源换代须遍历保留的语义图；普通语法 visitor 刻意排除的引用也必须参与来源迁移。 */
class CfirSourceGraphContractTest {
    @Test
    fun sourceGraphIncludesResolvedSubscriptCalls() {
        val get = buildFunctionCall { calleeReference = reference("get") }
        val set = buildFunctionCall { calleeReference = reference("set") }
        val subscript = buildSubscriptExpression {
            receiver = literal("1")
            indices += literal("2")
            resolvedGetCall = get
            resolvedSetCall = set
        }
        assertFalse(get in collect(subscript, sourceGraph = false))
        assertTrue(get in collect(subscript, sourceGraph = true))
        assertTrue(set in collect(subscript, sourceGraph = true))
    }

    @Test
    fun sourceGraphIncludesAnnotationMappingAndIndependentViewConstants() {
        val mapped = literal("1")
        val constant = literal("42")
        val mapping = CfirAnnotationArgumentMappingImpl(null, mapOf(Name.identifier("value") to mapped))
        val annotation = buildAnnotationCall {
            containingDeclarationSymbol = CfirFileSymbol()
            typeRef = org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef()
            calleeReference = reference("Anno")
            argumentList = CfirEmptyArgumentList
            argumentMapping = mapping
            argumentView = CfirAnnotationArgumentView(listOf(CfirAnnotationArgumentViewEntry(
                sourceOrder = 0, explicitName = null, argument = null, resolvedParameter = null,
                status = CfirAnnotationArgumentStatus.UNRESOLVED, isDefaultOrigin = false,
                constantExpression = constant, source = null,
            )))
        }
        assertFalse(mapping in collect(annotation, sourceGraph = false))
        val graph = collect(annotation, sourceGraph = true)
        assertTrue(mapping in graph)
        assertTrue(mapped in graph)
        assertTrue(constant in graph)
    }

    @Test
    fun sourceGraphIncludesOriginalUnmappedArguments() {
        val argument = literal("42")
        val original = buildArgumentList { arguments += argument }
        val resolved = org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentListImpl(original, linkedMapOf())
        assertFalse(argument in collect(resolved, sourceGraph = false))
        val graph = collect(resolved, sourceGraph = true)
        assertTrue(original in graph)
        assertTrue(argument in graph)
    }

    /** 默认值由形参声明拥有；annotation 视图借用它，不取得另一文件/会话的可变源所有权。 */
    @Test
    fun sourceGraphDoesNotOwnBorrowedAnnotationDefault() {
        val parameterDefault = literal("42")
        val annotation = buildAnnotationCall {
            containingDeclarationSymbol = CfirFileSymbol()
            typeRef = org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef()
            calleeReference = reference("Anno")
            argumentList = CfirEmptyArgumentList
            argumentView = CfirAnnotationArgumentView(listOf(CfirAnnotationArgumentViewEntry(
                sourceOrder = 0, explicitName = Name.identifier("value"), argument = null, resolvedParameter = null,
                status = CfirAnnotationArgumentStatus.UNRESOLVED, isDefaultOrigin = true,
                constantExpression = parameterDefault, source = null,
            )))
        }
        assertFalse(parameterDefault in collect(annotation, sourceGraph = true))
        assertTrue(parameterDefault in collect(parameterDefault, sourceGraph = true))
    }

    private fun literal(value: String): CfirLiteralExpression = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        this.value = value
    }

    private fun reference(name: String) = org.cangnova.cangjie.cfir.references.builder.buildNamedReference {
        this.name = Name.identifier(name)
    }

    private fun collect(root: CfirElement, sourceGraph: Boolean): Set<CfirElement> {
        val result = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        val method = if (sourceGraph) CfirElement::class.java.getMethod(
            "acceptSourceChildren", CfirVisitor::class.java, Any::class.java,
        ) else null
        root.accept(object : CfirVisitor<Unit, Set<CfirElement>>() {
            override fun visitElement(element: CfirElement, data: Set<CfirElement>) {
                if (!result.add(element)) return
                if (method != null) method.invoke(element, this, data) else element.acceptChildren(this, data)
            }
        }, result)
        return result
    }
}

package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResourceVariable
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.CfirCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeNode
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementKind
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertContains as checkAssertContains
import kotlin.test.assertEquals as checkAssertEquals
import kotlin.test.assertFalse as checkAssertFalse
import kotlin.test.assertIs as checkAssertIs
import kotlin.test.assertNotNull as checkAssertNotNull
import kotlin.test.assertNotSame as checkAssertNotSame
import kotlin.test.assertNull as checkAssertNull
import kotlin.test.assertSame as checkAssertSame
import kotlin.test.assertTrue as checkAssertTrue

/**
 * PSI 与 LightTree 分别验证模式树的唯一宿主、叶子身份和专用变量角色。
 *
 * 单名、嵌套 tuple、wildcard 与资源/catch 源码取自已由 cjc 验证的
 * patternBindingSymbols/ownerEntries.cj 和 hostBindingSymbols/{resources,catches}.cj。
 * 成员边界依据官方 class/struct 与属性文档；command 语法依据官方实验性 parser
 * ParseAtom.cpp::ParseCommandTypePattern 和 ToStringTest.cpp::VerifyCommandTypePatternRestore。
 * 这里只验收 raw 的声明、source 和子树结构，不要求 String、Exception 或 command 类型已经 resolve。
 */
@RunWith(JUnit3RunnerWithInners::class)
class PatternBindingRawCfirOwnershipTest : AbstractLightTree2CfirConverterTestCase() {
    fun testFileAndLocalDeclarationsKeepUniquePatternOwnersInBothBuilders() {
        forEachBuilder(PATTERN_DECLARATIONS) { file, builder ->
            val globals = file.declarations.filterIsInstance<CfirVariable>()
            val function = namedFunction(file, "localBindings")
            val locals = checkAssertNotNull(function.body).statements.filterIsInstance<CfirVariable>()
            val owners = listOf(false to globals, true to locals).flatMap { (isLocal, declarations) ->
                checkAssertEquals(3, declarations.size, "$builder: 每个作用域保留三个声明容器")
                val scopeOwners = declarations.map { checkAssertIs<CfirPatternVariable>(it) }
                val single = scopeOwners[0]
                val tuple = scopeOwners[1]
                val wildcards = scopeOwners[2]

                checkAssertEquals(listOf("singleValue"), bindingNames(single.pattern))
                checkAssertIs<CfirBindingPattern>(single.pattern)
                val singleInitializer = checkAssertIs<CfirLiteralExpression>(single.initializer)
                checkAssertEquals("1", sourceText(singleInitializer.source, PATTERN_DECLARATIONS))
                val singleBinding = collectElements<CfirPatternBindingVariable>(single.pattern).single()
                checkAssertSame(singleInitializer, singleBinding.initializer)

                val tuplePattern = checkAssertIs<CfirTuplePattern>(tuple.pattern)
                checkAssertEquals(3, tuplePattern.elements.size)
                checkAssertEquals("number", checkAssertIs<CfirBindingPattern>(tuplePattern.elements[0]).name.asString())
                val nested = checkAssertIs<CfirTuplePattern>(tuplePattern.elements[1])
                checkAssertEquals(2, nested.elements.size)
                checkAssertEquals(listOf("flag", "text"), nested.elements.map { checkAssertIs<CfirBindingPattern>(it).name.asString() })
                checkAssertIs<CfirWildcardPattern>(tuplePattern.elements[2])
                checkAssertEquals(listOf("number", "flag", "text"), bindingNames(tuple.pattern))
                val tupleInitializer = checkAssertIs<CfirTupleLiteral>(tuple.initializer)
                checkAssertEquals(3, tupleInitializer.elements.size)
                val nestedInitializer = checkAssertIs<CfirTupleLiteral>(tupleInitializer.elements[1])
                checkAssertEquals(2, nestedInitializer.elements.size)
                checkAssertEquals(CfirLiteralKind.STRING, checkAssertIs<CfirLiteralExpression>(nestedInitializer.elements[1]).kind)
                checkAssertEquals("(2, (true, \"ready\"), false)", sourceText(tupleInitializer.source, PATTERN_DECLARATIONS))

                val wildcardPattern = checkAssertIs<CfirTuplePattern>(wildcards.pattern)
                checkAssertEquals(2, wildcardPattern.elements.size)
                wildcardPattern.elements.forEach { checkAssertIs<CfirWildcardPattern>(it) }
                checkAssertTrue(collectElements<CfirVariable>(wildcards.pattern).isEmpty(), "$builder: wildcard 不产生 variable")
                checkAssertIs<CfirTupleLiteral>(wildcards.initializer)

                scopeOwners.forEach { owner ->
                    checkAssertFalse(owner.isVar)
                    checkAssertEquals(isLocal, owner.isLocal)
                    assertSourceDeclaration(owner, PATTERN_DECLARATIONS)
                    assertTreeOwner(owner, owner.pattern, PATTERN_DECLARATIONS)
                    collectElements<CfirPatternBindingVariable>(owner.pattern).forEach { binding ->
                        checkAssertEquals(isLocal, binding.isLocal)
                        checkAssertFalse(binding.isVar)
                        assertSourceDeclaration(binding, PATTERN_DECLARATIONS)
                        checkAssertContains(
                            sourceText(binding.source, PATTERN_DECLARATIONS, CjFakeSourceElementKind.PatternBindingVariable),
                            binding.name.asString(),
                        )
                    }
                    // 叶子的 initializer 是语义引用，整份 initializer 子树只能由容器遍历一次。
                    checkAssertEquals(1, collectElements<CfirExpression>(owner).count { it === owner.initializer })
                }
                scopeOwners
            }

            val bindings = owners.flatMap { collectElements<CfirPatternBindingVariable>(it.pattern) }
            checkAssertEquals(8, bindings.size, "$builder: 同名的文件/局部叶子不能合并")
            assertDistinctIdentities(owners)
            assertDistinctIdentities(owners.map { it.pattern })
            assertDistinctIdentities(bindings)
            assertDistinctIdentities(bindings.map { it.symbol })
        }
    }

    fun testClassAndStructMembersRetainFieldAndPropertyRolesInBothBuilders() {
        forEachBuilder(MEMBER_DECLARATIONS) { file, builder ->
            val hosts = file.declarations.filterIsInstance<CfirClassLikeDeclaration>()
            checkAssertEquals(listOf("Holder", "Record"), hosts.map { it.name.asString() })
            checkAssertIs<CfirClass>(hosts[0])
            checkAssertIs<CfirStruct>(hosts[1])
            val members = hosts.flatMap { host ->
                val fields = host.declarations.filterIsInstance<CfirVariable>().map { checkAssertIs<CfirFieldVariable>(it) }
                val properties = host.declarations.filterIsInstance<CfirProperty>()
                checkAssertEquals(listOf("field", "total"), fields.map { it.name.asString() }, builder)
                checkAssertEquals(listOf("value", "maximum"), properties.map { it.name.asString() }, builder)
                checkAssertFalse(fields[0].status.isStatic)
                checkAssertTrue(fields[1].status.isStatic)
                checkAssertFalse(properties[0].status.isStatic)
                checkAssertTrue(properties[1].status.isStatic)
                fields.forEachIndexed { index, field ->
                    checkAssertFalse(field.isLocal)
                    checkAssertTrue(field.isVar)
                    checkAssertEquals((index + 1).toString(), sourceText(checkAssertNotNull(field.initializer).source, MEMBER_DECLARATIONS))
                    checkAssertIs<CfirLiteralExpression>(field.initializer)
                }
                properties.forEach { property ->
                    checkAssertFalse(property.isLocal)
                    checkAssertNotNull(checkAssertNotNull(property.getter).body)
                    checkAssertNull(property.setter)
                }
                val sourceMembers: List<CfirDeclaration> = fields + properties
                sourceMembers.forEach { member ->
                    assertSourceDeclaration(member, MEMBER_DECLARATIONS)
                    checkAssertFalse(member is CfirPatternTreeOwner, "$builder: 成员不能成为模式声明容器")
                    checkAssertFalse(member is CfirPatternTreeNode, "$builder: 成员不能冒充绑定叶子")
                }
                checkAssertTrue(collectElements<CfirPatternBindingVariable>(host).isEmpty(), "$builder: 成员不产生同名绑定叶子")
                sourceMembers
            }
            checkAssertEquals(8, members.size)
            assertDistinctIdentities(members)
            assertDistinctIdentities(members.map { it.symbol })
        }
    }

    fun testResourceInitializersAndDedicatedCatchBindingsSurviveBothBuilders() {
        forEachBuilder(RESOURCE_AND_CATCH_DECLARATIONS) { file, builder ->
            val resourceTry = collectElements<CfirTryExpression>(namedFunction(file, "useResources")).single()
            val resources = resourceTry.resources
            checkAssertEquals(listOf("first", "second"), resources.map { it.name.asString() }, builder)
            resources.forEach { resource ->
                checkAssertIs<CfirResourceVariable>(resource)
                checkAssertTrue(resource.isLocal)
                checkAssertFalse(resource.isVar)
                assertSourceDeclaration(resource, RESOURCE_AND_CATCH_DECLARATIONS)
                checkAssertFalse(resource is CfirPatternTreeOwner)
                checkAssertFalse(resource is CfirPatternTreeNode)
                checkAssertEquals(1, collectElements<CfirExpression>(resource).count { it === resource.initializer })
            }
            val firstInitializer = checkAssertIs<CfirFunctionCall>(resources[0].initializer)
            checkAssertEquals("ManagedResource", checkAssertIs<CfirNamedReference>(firstInitializer.calleeReference).name.asString())
            checkAssertEquals("ManagedResource()", sourceText(firstInitializer.source, RESOURCE_AND_CATCH_DECLARATIONS))
            val secondInitializer = checkAssertIs<CfirQualifiedAccessExpression>(resources[1].initializer)
            checkAssertEquals("first", checkAssertIs<CfirNamedReference>(secondInitializer.calleeReference).name.asString())
            checkAssertEquals("first", sourceText(secondInitializer.source, RESOURCE_AND_CATCH_DECLARATIONS))
            assertDistinctIdentities(resources)
            assertDistinctIdentities(resources.map { it.symbol })
            assertDistinctIdentities(resources.map { checkAssertNotNull(it.initializer) })

            val bodyOwners = resourceTry.tryBlock.statements.filterIsInstance<CfirVariable>().map { checkAssertIs<CfirPatternVariable>(it) }
            checkAssertEquals(listOf("one", "two"), bodyOwners.flatMap { bindingNames(it.pattern) })
            bodyOwners.forEach { assertTreeOwner(it, it.pattern, RESOURCE_AND_CATCH_DECLARATIONS) }
            checkAssertEquals(listOf("first", "second"), bodyOwners.map { sourceText(checkAssertNotNull(it.initializer).source, RESOURCE_AND_CATCH_DECLARATIONS) })

            val catches = collectElements<CfirTryExpression>(namedFunction(file, "useCatch")).map { it.catches.single() }
            checkAssertEquals(2, catches.size)
            catches.forEach { caught ->
                val pattern = checkAssertIs<CfirCatchPattern>(caught.pattern)
                assertTreeOwner(caught, pattern, RESOURCE_AND_CATCH_DECLARATIONS)
                checkAssertEquals(listOf("Exception"), pattern.typeRefs.map { sourceText(it.source, RESOURCE_AND_CATCH_DECLARATIONS) })
                checkAssertNull(pattern.resolvedTypeRef)
            }
            val namedPattern = catches[0].pattern
            checkAssertFalse(namedPattern.isWildcard)
            checkAssertEquals("failure", checkAssertNotNull(namedPattern.bindingName).asString())
            val failure = checkAssertNotNull(namedPattern.bindingVariable)
            assertSpecialHostBinding(failure, catches[0], "failure", RESOURCE_AND_CATCH_DECLARATIONS)
            checkAssertSame(failure, collectElements<CfirPatternBindingVariable>(namedPattern).single())
            val wildcardPattern = catches[1].pattern
            checkAssertTrue(wildcardPattern.isWildcard)
            checkAssertNull(wildcardPattern.bindingVariable)
            checkAssertTrue(collectElements<CfirVariable>(wildcardPattern).isEmpty(), "$builder: catch wildcard 不产生 variable")
            checkAssertNotSame(namedPattern, wildcardPattern)
            assertDistinctIdentities(resources.map { it.symbol } + failure.symbol)
        }
    }

    fun testCommandPatternsKeepDedicatedOwnersAndWildcardStructureInBothBuilders() {
        forEachBuilder(COMMAND_DECLARATIONS) { file, builder ->
            val handlers = collectElements<CfirTryExpression>(namedFunction(file, "commandBindings")).map { it.handlers.single() }
            checkAssertEquals(2, handlers.size, builder)
            handlers.forEach { handler ->
                val pattern = checkAssertIs<CfirCommandTypePattern>(handler.commandPattern)
                assertTreeOwner(handler, pattern, COMMAND_DECLARATIONS)
                checkAssertNull(pattern.resolvedTypeRef)
            }
            val namedPattern = handlers[0].commandPattern
            checkAssertFalse(namedPattern.isWildcard)
            checkAssertEquals("effect", checkAssertNotNull(namedPattern.bindingName).asString())
            checkAssertEquals(listOf("Effect1", "Effect2"), namedPattern.typeRefs.map { sourceText(it.source, COMMAND_DECLARATIONS) })
            val effect = checkAssertNotNull(namedPattern.bindingVariable)
            assertSpecialHostBinding(effect, handlers[0], "effect", COMMAND_DECLARATIONS)
            checkAssertSame(effect, collectElements<CfirPatternBindingVariable>(namedPattern).single())
            val wildcardPattern = handlers[1].commandPattern
            checkAssertTrue(wildcardPattern.isWildcard)
            checkAssertEquals(listOf("Effect1"), wildcardPattern.typeRefs.map { sourceText(it.source, COMMAND_DECLARATIONS) })
            checkAssertNull(wildcardPattern.bindingVariable)
            checkAssertTrue(collectElements<CfirVariable>(wildcardPattern).isEmpty(), "$builder: command wildcard 不产生 variable")
            assertDistinctIdentities(handlers)
            checkAssertNotSame(namedPattern, wildcardPattern)
        }
    }

    /** 每个 builder 使用独立 session；按同一份源码契约检查，不能仅比较两份可能同时出错的 dump。 */
    private fun forEachBuilder(text: String, assertions: (CfirFile, String) -> Unit) {
        for (builder in RawBuilder.entries) {
            val session = createTestSession()
            val fileName = "patternBindingOwnership${builder.name}"
            val file = when (builder) {
                RawBuilder.PSI -> PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
                    .buildCfirFile(createCjFile(fileName, text))
                RawBuilder.LIGHT_TREE -> LightTree2Cfir(session, session.cangjieScopeProvider)
                    .buildCfirFileWithSurfaces(
                        parseLightTree(text), CjInMemoryTextSourceFile("$fileName.cj", null, text), text.toSourceLinesMapping(),
                    ).first
            }
            checkAssertTrue(collectElements<CfirErrorExpression>(file).isEmpty(), "$builder: 合法语法不能产生 raw error expression")
            assertions(file, builder.name)
        }
    }

    /** 直接遍历真实子节点，包含每个嵌套 pattern 与叶子；反向 owner 不参与遍历。 */
    private fun assertTreeOwner(owner: CfirPatternTreeOwner, root: CfirElement, text: String) {
        checkAssertSame(root, owner.patternTreeRoot())
        val nodes = collectElements<CfirPatternTreeNode>(root)
        checkAssertTrue(nodes.isNotEmpty())
        nodes.forEach { node ->
            checkAssertSame(owner, node.patternTreeOwner)
            val element = node as CfirElement
            sourceText(element.source, text, sourceKindFor(element))
        }
        assertDistinctIdentities(nodes)
    }

    private fun assertSpecialHostBinding(binding: CfirPatternBindingVariable, owner: CfirPatternTreeOwner, name: String, text: String) {
        checkAssertSame(owner, binding.patternTreeOwner)
        checkAssertEquals(name, binding.name.asString())
        checkAssertTrue(binding.isLocal)
        checkAssertFalse(binding.isVar)
        checkAssertNull(binding.initializer)
        assertSourceDeclaration(binding, text)
        checkAssertContains(sourceText(binding.source, text, CjFakeSourceElementKind.PatternBindingVariable), name)
    }

    private fun assertSourceDeclaration(declaration: CfirDeclaration, text: String) {
        checkAssertSame(CfirDeclarationOrigin.Source, declaration.origin)
        checkAssertSame(declaration, declaration.symbol.cfir)
        sourceText(declaration.source, text, sourceKindFor(declaration))
    }

    /** 语法节点保持真实 source，内部变量以明确的 PatternBindingVariable kind 锚到规范节点。 */
    private fun sourceKindFor(element: CfirElement): CjSourceElementKind =
        if (element is CfirPatternBindingVariable) CjFakeSourceElementKind.PatternBindingVariable else CjRealSourceElementKind

    private fun sourceText(
        source: CjSourceElement?,
        text: String,
        expectedKind: CjSourceElementKind = CjRealSourceElementKind,
    ): String {
        val actual = checkAssertNotNull(source)
        checkAssertSame(expectedKind, actual.kind)
        checkAssertTrue(actual.startOffset in 0 until actual.endOffset)
        checkAssertTrue(actual.endOffset <= text.length)
        return text.substring(actual.startOffset, actual.endOffset)
    }

    private fun namedFunction(file: CfirFile, name: String): CfirNamedFunction =
        file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == name }

    private fun bindingNames(root: CfirElement): List<String> =
        collectElements<CfirPatternBindingVariable>(root).map { it.name.asString() }

    private fun assertDistinctIdentities(elements: List<Any>) {
        for (index in elements.indices) {
            for (other in index + 1 until elements.size) {
                checkAssertNotSame(elements[index], elements[other])
            }
        }
    }

    private inline fun <reified T> collectElements(root: CfirElement): List<T> {
        val elements = mutableListOf<T>()
        root.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is T) elements += element
                element.acceptChildren(this)
            }
        })
        return elements
    }

    private enum class RawBuilder { PSI, LIGHT_TREE }

    private companion object {
        val PATTERN_DECLARATIONS = """
            package test.patterns.symbols

            let singleValue = 1
            let (number, (flag, text), _) = (2, (true, "ready"), false)
            let (_, _) = (3, true)

            func localBindings(): Unit {
                let singleValue = 1
                let (number, (flag, text), _) = (2, (true, "ready"), false)
                let (_, _) = (3, true)
            }
        """.trimIndent()

        val MEMBER_DECLARATIONS = """
            class Holder {
                public var field: Int64 = 1
                public static var total: Int64 = 2
                public prop value: Int64 { get() { field } }
                public static prop maximum: Int64 { get() { total } }
            }
            struct Record {
                public var field: Int64 = 1
                public static var total: Int64 = 2
                public prop value: Int64 { get() { field } }
                public static prop maximum: Int64 { get() { total } }
            }
        """.trimIndent()

        val RESOURCE_AND_CATCH_DECLARATIONS = """
            package test.hosts

            class ManagedResource <: Resource {
                public func isClosed(): Bool { false }
                public func close(): Unit {}
            }

            func useResources(): Unit {
                try (first = ManagedResource(), second = first) {
                    let one = first
                    let two = second
                }
            }

            func useCatch(): Unit {
                try {
                    throw Exception()
                } catch (failure: Exception) {
                    let captured = failure
                }
                try {
                    throw Exception()
                } catch (_: Exception) {}
            }
        """.trimIndent()

        // 官方 experimental parser 覆盖命名联合类型；wildcard 由 ParseCommandTypePattern 的专用分支接受。
        // Effect1/Effect2 仅是待解析的类型语法，不在 raw 测试中声称它们满足 command 的类型约束。
        val COMMAND_DECLARATIONS = """
            func commandBindings() {
                try {} handle (effect: Effect1 | Effect2) {}
                try {} handle (_: Effect1) {}
            }
        """.trimIndent()
    }
}

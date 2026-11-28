@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.*

/**
 * 声明 decompiler 的模式合法域：保留真实结构，在创建 variable stub 前递归拒绝非法模式。
 * 直接调用生产声明 dispatcher；这是完成 CFIR header 输入，不冒称 Native ABI 保存完整解构容器。
 * 固定依据见 build/cjmp-body-reader-20261122/decompiler-pattern-authority.md。
 */
class DeclarationPatternStubTest {
    @Test
    fun bindingKeepsItsNameReferenceAndQualifiedName() {
        val fixture = Fixture()
        val pattern = fixture.binding("汉")
        val root = file()
        createDeclarationStub(root, fixture.declaration(pattern), context())
        val variable = variable(root)
        assertEquals(PatternKind.BINDING, variable.getPatternKind())
        val binding = assertIs<CangJieBindingPatternStubImpl>(patternChildren(variable).single())
        assertEquals("汉", binding.getName())
        assertEquals(FqName("patterns.汉"), binding.fqName)
        assertEquals(listOf("汉"), references(binding))
        assertEquals("binding(汉)", shape(binding))
        assertSame(pattern.bindingVariable, pattern.bindingVariable!!.symbol.cfir)
        assertNotNull(variable.findChildStubByType(CjStubElementTypes.TYPE_REFERENCE))
    }

    @Test
    fun wildcardHasNoNamedBindingStub() {
        val fixture = Fixture()
        val root = file()
        createDeclarationStub(root, fixture.declaration(fixture.wildcard()), context())
        val variable = variable(root)
        assertEquals(PatternKind.WILDCARD, variable.getPatternKind())
        assertEquals("wildcard", shape(patternChildren(variable).single()))
        assertTrue(descendants(variable).filterIsInstance<CangJieBindingPatternStubImpl>().isEmpty())
        assertTrue(descendants(variable).filterIsInstance<CangJieNameReferenceExpressionStubImpl>().isEmpty())
    }

    @Test
    fun recursiveTupleKeepsEveryChildInOrderAndDeclarationFlags() {
        for ((isVar, isConst) in listOf(false to false, true to false, false to true)) {
            val fixture = Fixture()
            val pattern = fixture.tuple(fixture.binding("first", mutable = isVar, constant = isConst),
                fixture.tuple(fixture.wildcard(), fixture.binding("last", mutable = isVar, constant = isConst)))
            val root = file()
            createDeclarationStub(root, fixture.declaration(pattern, isVar, isConst), context())
            val variable = variable(root)
            assertEquals(PatternKind.TUPLE, variable.getPatternKind())
            assertEquals(isVar, variable.isVar())
            assertEquals(isConst, variable.isConst())
            assertEquals("tuple(binding(first),tuple(wildcard,binding(last)))", shape(patternChildren(variable).single()))
            assertEquals(listOf("first", "last"),
                descendants(variable).filterIsInstance<CangJieBindingPatternStubImpl>().map { it.getName() })
            assertEquals(listOf(FqName("patterns.first"), FqName("patterns.last")),
                descendants(variable).filterIsInstance<CangJieBindingPatternStubImpl>().map { it.fqName })
            assertNotNull(variable.findChildStubByType(CjStubElementTypes.TYPE_REFERENCE))
        }
    }

    @Test
    fun singleConstructorEnumKeepsConstructorReferenceAndRecursiveArgument() {
        val fixture = Fixture()
        val pattern = fixture.enumPattern(fixture.tuple(fixture.binding("payload"), fixture.wildcard()))
        val root = file()
        createDeclarationStub(root, fixture.declaration(pattern), context())
        val variable = variable(root)
        assertEquals(PatternKind.ENUM, variable.getPatternKind())
        val enum = assertIs<CangJieEnumPatternStubImpl>(patternChildren(variable).single())
        assertEquals(listOf("Only"), references(enum))
        assertEquals("enum(Only;tuple(binding(payload),wildcard))", shape(enum))
        assertEquals(listOf("payload"), descendants(enum).filterIsInstance<CangJieBindingPatternStubImpl>().map { it.getName() })
    }

    /** 非法节点放在根、已有合法兄弟后的 tuple、单构造 enum 内，分别检查递归与原子拒绝。 */
    @TestFactory
    fun forbiddenKindsFailBeforeCreatingAnyVariableStub(): List<DynamicTest> =
        Forbidden.entries.flatMap { kind ->
            listOf("root", "nested tuple", "enum argument").map { position ->
                DynamicTest.dynamicTest("$kind at $position") {
                    val fixture = Fixture()
                    val invalid = fixture.forbidden(kind)
                    val pattern = when (position) {
                        "root" -> invalid
                        "nested tuple" -> fixture.tuple(fixture.binding("earlier"),
                            fixture.tuple(fixture.wildcard(), invalid))
                        "enum argument" -> fixture.enumPattern(invalid)
                        else -> error("Unknown test position")
                    }
                    val root = file()
                    createDeclarationStub(root, fixture.declaration(fixture.binding("existing")), context())
                    assertRejected(root, fixture.declaration(pattern), invalid, context())
                }
            }
        }

    /** Native ParseDecl 拒绝把顶层/局部解构语法推广到 class/struct 成员。 */
    @TestFactory
    fun destructuringPatternsAreRejectedInNominalMemberPosition(): List<DynamicTest> =
        listOf("wildcard", "tuple", "enum").map { kind ->
            DynamicTest.dynamicTest(kind) {
                val fixture = Fixture()
                val pattern = when (kind) {
                    "wildcard" -> fixture.wildcard()
                    "tuple" -> fixture.tuple(fixture.binding("member"), fixture.wildcard())
                    "enum" -> fixture.enumPattern(fixture.binding("member"))
                    else -> error("Unknown test pattern")
                }
                val root = file()
                val holder = CangJieStructStubImpl(CjStubElementTypes.STRUCT, root,
                    com.intellij.util.io.StringRef.fromString("patterns.Holder"), null,
                    com.intellij.util.io.StringRef.fromString("Holder"), emptyArray())
                val body = CangJiePlaceHolderStubImpl<CjAbstractClassBody>(holder, CjStubElementTypes.CLASS_BODY)
                assertRejected(body, fixture.declaration(pattern), pattern, context().child(Name.identifier("Holder")))
            }
        }

    private fun assertRejected(parent: StubElement<*>, declaration: CfirPatternVariable,
        offending: CfirPattern, context: CjoStubBuilderContext) {
        val before = parent.childrenStubs.toList()
        val beforeShape = before.map(::treeShape)
        val failure = assertFails { createDeclarationStub(parent, declaration, context) }
        assertTrue(failure is IllegalArgumentException || failure is IllegalStateException,
            "Must explicitly reject the declaration pattern, got ${failure.javaClass.name}")
        val message = assertNotNull(failure.message)
        assertTrue(message.isNotBlank(), "Declaration-pattern rejection must explain the error")
        assertTrue(message.contains(offending.javaClass.simpleName.removeSuffix("Impl")),
            "The error must identify the offending pattern: $message")
        assertEquals(before.size, parent.childrenStubs.size, "No partial variable/pattern stub may survive rejection")
        before.forEachIndexed { index, stub -> assertSame(stub, parent.childrenStubs[index]) }
        assertEquals(beforeShape, parent.childrenStubs.map(::treeShape))
    }

    private fun file() = CangJieFileStubImpl.forFile(FqName("patterns"))
    private fun context() = CjoStubBuilderContext(FqName("patterns"))
    private fun variable(root: CangJieFileStubImpl): CangJieVariableStubImpl =
        assertIs<CangJieVariableStubImpl>(root.childrenStubs.single())

    private fun descendants(root: StubElement<*>): List<StubElement<*>> =
        root.childrenStubs.flatMap { listOf(it) + descendants(it) }

    private fun patternChildren(root: StubElement<*>): List<StubElement<*>> = root.childrenStubs.filter {
        it is CangJieBindingPatternStubImpl || it is CangJieWildcardPatternStubImpl ||
            it is CangJieTuplePatternStubImpl || it is CangJieEnumPatternStubImpl
    }

    private fun references(root: StubElement<*>): List<String> =
        root.childrenStubs.filterIsInstance<CangJieNameReferenceExpressionStubImpl>().map { it.getReferencedName() }

    private fun shape(stub: StubElement<*>): String = when (stub) {
        is CangJieBindingPatternStubImpl -> "binding(${stub.getName()})"
        is CangJieWildcardPatternStubImpl -> "wildcard"
        is CangJieTuplePatternStubImpl -> "tuple(${patternChildren(stub).joinToString(",") { shape(it) }})"
        is CangJieEnumPatternStubImpl -> "enum(${references(stub).single()};${patternChildren(stub).joinToString(",") { shape(it) }})"
        else -> error("Unexpected declaration pattern stub: ${stub.stubType}")
    }

    private fun treeShape(stub: StubElement<*>): String =
        "${stub.stubType}:${stub}[${stub.childrenStubs.joinToString { treeShape(it) }}]"

    private enum class Forbidden { TYPE, TYPE_WILDCARD, CONST, EXPRESSION, OR, VAR_OR_ENUM, CATCH, COMMAND }

    private class Fixture {
        private val packageName = FqName("patterns")
        private val session = object : CfirSession(Kind.Library) {}
        private val module: CfirModuleData = CfirSourceModuleData(Name.identifier("pattern-stubs"),
            emptyList(), emptyList(), CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT).also {
            it.bindSession(session)
        }
        private val intType = ConePrimitiveType.INT64

        fun binding(text: String, type: ConeCangJieType = intType,
            mutable: Boolean = false, constant: Boolean = false): CfirBindingPattern {
            val identifier = Name.identifier(text)
            val variable = buildPatternBindingVariable {
                moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library; attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false; isVar = mutable
                status = CfirDeclarationStatusImpl(Visibilities.Public).apply { isConst = constant }
                name = identifier; symbol = CfirPatternBindingSymbol(CallableId(packageName, identifier))
                returnTypeRef = type.toCfirResolvedTypeRef(null)
            }
            return buildBindingPattern {
                name = identifier; bindingVariable = variable
                semanticTypeRef = variable.returnTypeRef
            }
        }

        fun wildcard(): CfirWildcardPattern = buildWildcardPattern {
            semanticTypeRef = intType.toCfirResolvedTypeRef(null)
        }

        fun tuple(vararg children: CfirPattern): CfirTuplePattern = buildTuplePattern {
            elements += children
            semanticTypeRef = ConeTupleType(children.map { assertNotNull(it.semanticTypeRef).coneType }).toCfirResolvedTypeRef(null)
        }

        fun declaration(value: CfirPattern, mutable: Boolean = false, constant: Boolean = false): CfirPatternVariable =
            buildPatternVariable {
                moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library; attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false; isVar = mutable
                status = CfirDeclarationStatusImpl(Visibilities.Public).apply { isConst = constant }
                symbol = CfirPatternVariableSymbol(CallableId(packageName, Name.special("<pattern-variable>")))
                returnTypeRef = assertNotNull(value.semanticTypeRef); pattern = value
                // 完成 header 的 initializer 可不公开；这些输入不冒称完整 SOURCE 正文或 Native header 产物。
            }

        fun enumPattern(argument: CfirPattern): CfirEnumPattern {
            val id = ClassId(packageName, Name.identifier("Single"))
            val enumSymbol = CfirEnumSymbol(id, useFixedSymbolLookupTag = true)
            val enumType = ConeEnumType(enumSymbol.toLookupTag())
            val constructorSymbol = CfirEnumConstructorSymbol(CallableId(id, Name.identifier("Only")))
            val parameter = buildValueParameter {
                moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library; attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false; isNamed = false; name = Name.identifier("value")
                symbol = CfirValueParameterSymbol(CallableId(name)); containingDeclarationSymbol = constructorSymbol
                returnTypeRef = assertNotNull(argument.semanticTypeRef)
            }
            val constructor = buildEnumConstructor {
                moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library; attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false; status = CfirDeclarationStatusImpl(Visibilities.Public)
                symbol = constructorSymbol; name = constructorSymbol.name
                returnTypeRef = enumType.toCfirResolvedTypeRef(null); valueParameters += parameter
            }
            val owner = buildEnum {
                moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library; attributes = CfirDeclarationAttributes.EMPTY
                status = CfirDeclarationStatusImpl(Visibilities.Public); symbol = enumSymbol; name = id.shortClassName
                scopeProvider = CfirCangJieScopeProvider(); isRefEnum = false; isNonExhaustive = false
                declarations += constructor
            }
            assertSame(owner, enumSymbol.cfir)
            assertSame(constructor, constructorSymbol.cfir)
            assertEquals(listOf(constructor), owner.declarations.filterIsInstance<CfirEnumConstructor>())
            return buildEnumPattern {
                semanticTypeRef = enumType.toCfirResolvedTypeRef(null)
                this.constructor = buildNamedAccessExpression {
                    coneTypeOrNull = ConeFunctionType(listOf(parameter.returnTypeRef.coneType), enumType)
                    calleeReference = buildResolvedNamedReference {
                        name = constructorSymbol.name; resolvedSymbol = constructorSymbol
                    }
                }
                arguments += argument
            }
        }

        fun forbidden(kind: Forbidden): CfirPattern = when (kind) {
            Forbidden.TYPE -> buildTypePattern {
                semanticTypeRef = intType.toCfirResolvedTypeRef(null); typeRef = intType.toCfirResolvedTypeRef(null)
                nestedPattern = binding("typed"); matchingKind = CfirTypePatternMatchingKind.ALWAYS
                matchBeforeRuntime = true; needRuntimeTypeCheck = false
            }
            Forbidden.TYPE_WILDCARD -> buildTypePattern {
                semanticTypeRef = intType.toCfirResolvedTypeRef(null); typeRef = intType.toCfirResolvedTypeRef(null)
                nestedPattern = wildcard(); matchingKind = CfirTypePatternMatchingKind.ALWAYS
                matchBeforeRuntime = true; needRuntimeTypeCheck = false
            }
            Forbidden.CONST -> buildConstPattern {
                semanticTypeRef = intType.toCfirResolvedTypeRef(null); expression = integer()
            }
            Forbidden.EXPRESSION -> buildExpressionPattern {
                semanticTypeRef = intType.toCfirResolvedTypeRef(null); expression = integer()
            }
            Forbidden.OR -> buildOrPattern {
                semanticTypeRef = intType.toCfirResolvedTypeRef(null); alternatives += listOf(wildcard(), wildcard())
            }
            Forbidden.VAR_OR_ENUM -> buildVarOrEnumPattern {
                semanticTypeRef = intType.toCfirResolvedTypeRef(null); name = Name.identifier("unresolved")
            }
            Forbidden.CATCH -> buildCatchPattern {
                val exceptionType = ConeClassLikeType(ClassId.topLevel(FqName("patterns.TestException")).toLookupTag())
                semanticTypeRef = exceptionType.toCfirResolvedTypeRef(null); nestedPattern = binding("caught", exceptionType)
                typeRefs += exceptionType.toCfirResolvedTypeRef(null)
            }
            Forbidden.COMMAND -> buildCommandTypePattern {
                val commandType = ConeClassLikeType(ClassId.topLevel(FqName("patterns.GetValue")).toLookupTag())
                semanticTypeRef = commandType.toCfirResolvedTypeRef(null); nestedPattern = binding("handled", commandType)
                typeRefs += commandType.toCfirResolvedTypeRef(null)
            }
        }

        private fun integer() = buildLiteralExpression {
            kind = CfirLiteralKind.INT; value = "7"; coneTypeOrNull = intType
        }
    }
}

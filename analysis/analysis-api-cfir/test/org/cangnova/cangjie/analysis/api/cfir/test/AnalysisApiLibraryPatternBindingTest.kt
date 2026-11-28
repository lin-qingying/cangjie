package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchSession
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.idea.search.CangJieReferencesSearchExecutor
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull as assertJupiterNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * 真实 CJO 的模式绑定消费链：编译库不挂源码，声明 PSI、公开查询、pointer 与搜索共享叶子身份。
 *
 * 夹具已由官方 cjc 1.1.3 独立编译并运行。全局解构在 CJO 中展开为具名 VarDecl，
 * 每个 wire 记录对应一个单名声明容器，不能恢复成已经丢失的原 tuple 容器。
 */
class AnalysisApiLibraryPatternBindingTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/libraryPatternBindings",
) {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = AnalysisSessionMode.Normal,
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    private val libraryPackage = FqName("patternlib")
    private val globalUses = linkedMapOf(
        "count" to "countUse",
        "enabled" to "enabledUse",
        "title" to "titleUse",
        "limit" to "limitUse",
    )

    /** 每个编译声明容器只有一个叶子；声明名、使用处与包级 named provider 恢复相同身份。 */
    @Test
    fun globalLeafSymbolsAndCompiledPsi(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val library = requireBinaryLibrary(mainFile, mainModule, testServices)
        val bindings = compiledBindings(library.file)
        analyzeForTest(mainFile) {
            val owners = globalUses.map { (name, marker) ->
                val bindingPsi = bindings.getValue(name)
                val ownerPsi = assertNotNull(bindingPsi.getStrictParentOfType<CjPatternVariable>())
                val ownerDeclaration: CaSymbol = ownerPsi.symbol
                assertFalse(ownerDeclaration is CaCallableSymbol, "CJO 声明容器不能取得 callable 身份。")
                val owner = assertInstanceOf(CaPatternVariableSymbol::class.java, ownerDeclaration)
                val leaf = assertNotNull(bindingPsi.bindingSymbol)
                val usage = reference(mainFile, testServices, marker)
                val used = assertInstanceOf(CaPatternBindingSymbol::class.java, usage.resolveToSymbol())
                val named = assertInstanceOf(
                    CaPatternBindingSymbol::class.java,
                    getTopLevelCallableSymbols(libraryPackage, Name.identifier(name)).single(),
                )

                assertEquals(leaf, used, "使用处必须恢复声明的具名叶子：$name")
                assertEquals(leaf, named, "包级 callable 查询必须返回叶子：$name")
                assertEquals(listOf(leaf), owner.entries)
                assertNotEquals(owner, leaf)
                assertNotSame(owner, leaf)
                assertSame(ownerPsi, owner.psi)
                assertEquals(owner, leaf.containingDeclaration)
                assertEquals(!leaf.isLet, owner.isVar)
                assertEquals(leaf.isConst, owner.isConst)
                assertEquals(CaSymbolLocation.TOP_LEVEL, owner.location)
                assertEquals(CaSymbolVisibility.PUBLIC, owner.visibility)
                assertEquals(CaSymbolOrigin.LIBRARY, owner.origin)
                assertSame(library.module, owner.containingModule)

                listOf(leaf, used, named).forEach { symbol ->
                    assertGlobalFacts(name, symbol, library.module)
                    assertSame(bindingPsi, symbol.psi)
                }
                assertSame(bindingPsi, usage.mainReference.resolve())
                val identifier = assertNotNull(bindingPsi.nameIdentifier)
                assertSame(
                    bindingPsi,
                    CangJieReferenceProvidersService.getInstance(mainFile.project).getNamedDeclarationTarget(identifier),
                    "编译声明名必须发布规范 binder，不能发布容器或原 tuple。",
                )
                assertTrue(identifier.references.isEmpty(), "声明名不能计为变量使用。")
                owner
            }
            owners.forEachIndexed { index, owner ->
                owners.drop(index + 1).forEach { other ->
                    assertNotEquals(owner, other, "不同 wire 名字必须拥有独立声明容器。")
                    assertNotSame(owner.psi, other.psi)
                    assertNotEquals(owner.entries.single(), other.entries.single())
                }
            }
        }
    }

    /** 清空公开会话缓存后同时恢复容器和叶子 pointer，避免同一会话缓存掩盖恢复错误。 */
    @Test
    fun freshSessionPointerRestore(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val library = requireBinaryLibrary(mainFile, mainModule, testServices)
        val bindings = compiledBindings(library.file)
        val (previousToken, pointers) = analyzeForTest(mainFile) {
            token to globalUses.map { (name, marker) ->
                val binding = bindings.getValue(name)
                val ownerPsi = assertNotNull(binding.getStrictParentOfType<CjPatternVariable>())
                val leaf = assertInstanceOf(
                    CaPatternBindingSymbol::class.java,
                    reference(mainFile, testServices, marker).resolveToSymbol(),
                )
                assertEquals(binding.bindingSymbol, leaf)
                NamedPointers(name, leaf.createPointer(), ownerPsi.symbol.createPointer())
            }
        }

        CaSessionProvider.getInstance(mainFile.project).clearCaches()

        analyzeForTest(mainFile) {
            // 只比较旧 token 的对象身份，不读取旧会话中失效的 symbol 或 type。
            assertNotSame(previousToken, token, "pointer 验收必须进入新 Analysis API 会话。")
            val restoredLeaves = pointers.map { record ->
                val leaf = assertInstanceOf(CaPatternBindingSymbol::class.java, restoreSymbol(record.leaf))
                val owner = assertInstanceOf(CaPatternVariableSymbol::class.java, restoreSymbol(record.owner))
                val binding = bindings.getValue(record.name)
                assertGlobalFacts(record.name, leaf, library.module)
                assertSame(binding, leaf.psi)
                assertSame(binding.getStrictParentOfType<CjPatternVariable>(), owner.psi)
                assertEquals(listOf(leaf), owner.entries)
                assertEquals(owner, leaf.containingDeclaration)
                assertNotEquals(owner, leaf)
                assertEquals(binding.bindingSymbol, leaf)
                assertEquals(
                    leaf,
                    reference(mainFile, testServices, globalUses.getValue(record.name)).resolveToSymbol(),
                )
                assertEquals(
                    leaf,
                    getTopLevelCallableSymbols(libraryPackage, Name.identifier(record.name)).single(),
                )
                leaf
            }
            assertEquals(globalUses.size, restoredLeaves.toSet().size, "恢复不得把不同名字合并为一个符号。")
        }
    }

    /** 搜索涵盖两个 alias 的读写和 import；库字段及 enum constructor 保留各自符号族。 */
    @Test
    fun aliasesSearchAndMemberFamilies(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val library = requireBinaryLibrary(mainFile, mainModule, testServices)
        val target = compiledBindings(library.file).getValue("count")
        val expected = listOf("firstImport", "secondImport", "firstWrite", "firstRead", "countUse")
            .map { reference(mainFile, testServices, it) }
        val usages = findUsages(target)
        assertEquals(5, usages.size, "两个 import、一次写入、两次读取必须完整且不重复。实际：${usages.map { it.element.text to it.rangeInElement.shiftRight(it.element.textRange.startOffset) }}")
        assertEquals(
            expected.map { it.containingFile to it.textRange }.toSet(),
            usages.map { it.element.containingFile to it.rangeInElement.shiftRight(it.element.textRange.startOffset) }.toSet(),
            "搜索位置必须精确匹配，不能包含局部遮盖、声明名或兄弟叶子。",
        )
        usages.forEach { usage ->
            assertTrue(usage.isReferenceTo(target))
            assertSame(target, usage.resolve())
        }

        val shadow = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjBindingPattern>(mainFile, "shadowDeclaration")
        val shadowUse = reference(mainFile, testServices, "shadowUse")
        assertSame(shadow, shadowUse.mainReference.resolve())
        assertFalse(shadowUse.mainReference.isReferenceTo(target))
        assertNotSame(target, shadow)

        analyzeForTest(mainFile) {
            val leaf = assertNotNull(target.bindingSymbol)
            expected.forEach { usage -> assertEquals(leaf, usage.resolveToSymbol()) }
            val local = assertNotNull(shadow.bindingSymbol)
            assertEquals(CaSymbolLocation.LOCAL, local.location)
            assertNotEquals(leaf, local)

            listOf("memberUse" to false, "sharedUse" to true).forEach { (marker, isStatic) ->
                val use = reference(mainFile, testServices, marker)
                val field = assertInstanceOf(CaFieldSymbol::class.java, use.resolveToSymbol())
                val fieldPsi = assertInstanceOf(CjFieldVariable::class.java, field.psi)
                assertSame(library.file, fieldPsi.containingFile)
                assertSame(fieldPsi, use.mainReference.resolve())
                assertEquals(field, fieldPsi.symbol)
                assertEquals(isStatic, field.isStatic)
                assertFalse(field.isLet)
                assertFalse(field.isConst)
                assertEquals(CaSymbolVisibility.PUBLIC, field.visibility)
                assertEquals(CaSymbolOrigin.LIBRARY, field.origin)
                assertEquals(CaSymbolLocation.CLASS, field.location)
                assertSame(library.module, field.containingModule)
                assertEquals(
                    Name.identifier("Holder"),
                    assertInstanceOf(CaClassLikeSymbol::class.java, field.containingDeclaration).name,
                )
                assertEquals(PrimitiveTypeKind.INT64, assertInstanceOf(CaPrimitiveType::class.java, field.returnType).kind)
            }
            val constructor = assertInstanceOf(
                CaEnumConstructorSymbol::class.java,
                reference(mainFile, testServices, "readyUse").resolveToSymbol(),
            )
            val constructorPsi = assertInstanceOf(CjEnumConstructor::class.java, constructor.psi)
            assertSame(library.file, constructorPsi.containingFile)
            assertEquals(constructor, constructorPsi.symbol)
            assertEquals(constructor, reference(mainFile, testServices, "readyPattern").resolveToSymbol())
            assertSame(constructorPsi, reference(mainFile, testServices, "readyUse").mainReference.resolve())
            assertSame(constructorPsi, reference(mainFile, testServices, "readyPattern").mainReference.resolve())
            assertEquals(ClassId(libraryPackage, Name.identifier("State")), constructor.containingEnumClassId)
            assertTrue(constructor.payloadTypes.isEmpty())
            assertEquals(CaSymbolOrigin.LIBRARY, constructor.origin)
            assertEquals(CaSymbolLocation.CLASS, constructor.location)
            assertSame(library.module, constructor.containingModule)
        }
    }

    /** 显式核验真实 binary roots 和模块依赖，杜绝源码 PSI 冒充编译库。 */
    private fun requireBinaryLibrary(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ): BinaryLibrary {
        val structure = testServices.cjTestModuleStructure
        val library = structure.getModule("lib")
        assertEquals(TestModuleKind.LibraryBinaryDecompiled, library.moduleKind)
        assertEquals(TestModuleKind.Source, mainModule.moduleKind)
        val binaryModule = assertInstanceOf(CaLibraryModule::class.java, library.caModule)
        assertTrue(binaryModule in mainModule.caModule.directRegularDependencies)
        val artifact = assertNotNull(testServices.compiledLibraryProvider.getCompiledLibrary("lib"))
        assertTrue(artifact.roots.isNotEmpty())
        assertTrue(artifact.sourceRoots.isEmpty(), "二进制库验收不能挂载源码 roots。")
        assertEquals(
            artifact.roots.map { it.toAbsolutePath().normalize() }.toSet(),
            binaryModule.binaryRoots.map { Path.of(it.virtualFile.path).toAbsolutePath().normalize() }.toSet(),
        )
        assertEquals(listOf(mainFile), structure.allCjFiles, "源码 provider 只能看见消费者。")
        val decompiled = assertInstanceOf(CjDecompiledFile::class.java, library.cjFiles.single())
        assertTrue(decompiled.isCompiled)
        assertTrue(decompiled.isPhysical)
        assertTrue(decompiled.virtualFile.path.endsWith("patternlib.cjo"))
        assertSame(library, structure.requireModuleByFile(decompiled))
        return BinaryLibrary(binaryModule, decompiled)
    }

    /** CJO 中的每个 global wire 记录投影为一个单名容器，两个原 tuple 叶子也必须分离。 */
    private fun compiledBindings(file: CjFile): Map<String, CjBindingPattern> {
        val owners = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
        assertEquals(globalUses.size, owners.size)
        val bindings = owners.map { owner ->
            PsiTreeUtil.findChildrenOfType(owner, CjBindingPattern::class.java).single()
        }
        val byName = bindings.associateBy { assertNotNull(it.name) }
        assertEquals(globalUses.keys, byName.keys)
        assertEquals(globalUses.size, byName.size)
        return byName
    }

    /** 只根据官方已编译的声明事实检查 mutability、const、类型和作用域。 */
    private fun CaSession.assertGlobalFacts(name: String, symbol: CaPatternBindingSymbol, module: CaLibraryModule) {
        assertEquals(Name.identifier(name), symbol.name)
        assertEquals(CallableId(libraryPackage, Name.identifier(name)), symbol.callableId)
        assertEquals(CaSymbolLocation.TOP_LEVEL, symbol.location)
        assertEquals(CaSymbolVisibility.PUBLIC, symbol.visibility)
        assertEquals(CaSymbolOrigin.LIBRARY, symbol.origin)
        assertSame(module, symbol.containingModule)
        assertEquals(name == "title" || name == "limit", symbol.isLet)
        assertEquals(name == "limit", symbol.isConst, "$name 必须保留官方 const 状态；PSI=${symbol.psi?.parent?.text}")
        assertFalse(symbol.returnType.isErrorType)
        when (name) {
            "title" -> assertEquals(
                ClassId(FqName("std.core"), Name.identifier("String")),
                assertInstanceOf(CaClassLikeType::class.java, symbol.returnType).classId,
            )
            "enabled" -> assertEquals(
                PrimitiveTypeKind.BOOLEAN,
                assertInstanceOf(CaPrimitiveType::class.java, symbol.returnType).kind,
            )
            "count", "limit" -> assertEquals(
                PrimitiveTypeKind.INT64,
                assertInstanceOf(CaPrimitiveType::class.java, symbol.returnType).kind,
            )
            else -> error("未声明的测试叶子：$name")
        }
    }

    private fun reference(file: CjFile, services: TestServices, marker: String): CjSimpleNameExpression =
        services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(file, marker)

    /** JUnit 非空断言返回 void；保留输入的具体类型供后续 PSI 和库根查询使用。 */
    private fun <T : Any> assertNotNull(value: T?, message: String? = null): T {
        assertJupiterNotNull(value, message)
        return requireNotNull(value)
    }

    /** 使用项目正式 ReferencesSearch executor，保留目标自身的 useScope。 */
    private fun findUsages(target: PsiElement): List<PsiReference> {
        val references = mutableListOf<PsiReference>()
        val parameters = ReferencesSearch.SearchParameters(
            target,
            target.useScope,
            false,
            SearchRequestCollector(SearchSession(target)),
        )
        CangJieReferencesSearchExecutor().execute(parameters, Processor { reference ->
            references += reference
            true
        })
        return references
    }

    private data class BinaryLibrary(val module: CaLibraryModule, val file: CjFile)

    private data class NamedPointers(
        val name: String,
        val leaf: CaSymbolPointer<CaSymbol>,
        val owner: CaSymbolPointer<CaSymbol>,
    )
}

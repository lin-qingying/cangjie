@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaDanglingFileModuleImpl
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CaMutableTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationMetadataOwner
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.ownExtendProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * G4：真实源码依赖的扩展必须在调用者的首次 LL 诊断请求中可用，并保留声明来源。
 *
 * 源码与访问边界取自官方 v1.1.3 的 g4-source-dependency/evidence.md；friend、refinement
 * 和普通依赖环只由平台模块图承载，不注入 session/provider，也不预热扩展声明文件。
 */
class CfirSourceExtendDependenciesTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/sourceExtendDependencies",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /** 首次发布与后续全量重建均推进真实代次；新代退役旧父缓存，保留同一源码产品。 */
    @Test
    fun lazyExtendIndexPublishesVersionAndCachesCompletedParents(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val session = file.moduleData.session
        val sourceProvider = session.ownExtendProvider
        val initialVersion = sourceProvider.modificationCount
        val receiver = file.declarations.filterIsInstance<CfirClass>().single()
        val expectedExtend = file.declarations.filterIsInstance<CfirExtend>().single()
        val supertypeProvider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        val receiverType = receiver.symbol.constructType()

        val parents = supertypeProvider.getDirectSupertypeDescriptors(receiverType)
        val inherited = parents.single { it.origin is CfirInstantiatedSupertypeOrigin.Extend }
        val origin = inherited.origin as CfirInstantiatedSupertypeOrigin.Extend
        val parentType = inherited.type as ConeClassLikeType
        assertEquals("VisibleMarker", parentType.classId.shortClassName.asString())
        assertTrue(parentType.isInterface)
        assertSame(expectedExtend, origin.sourceExtend)
        assertSame(file, origin.sourceExtend.getContainingFile())
        val completedVersion = sourceProvider.modificationCount
        assertTrue(completedVersion > initialVersion, "首次源码索引发布必须对缓存暴露真实代次")

        val cachedParents = supertypeProvider.getDirectSupertypeDescriptors(receiverType)
        assertSame(parents, cachedParents)
        assertEquals(completedVersion, sourceProvider.modificationCount)

        // 使用实际 LL 全量查询完成第二次索引发布，不反射或手写 cache/revision 状态。
        assertSame(expectedExtend, sourceProvider.getAllExtends().single())
        val rebuiltVersion = sourceProvider.modificationCount
        assertTrue(rebuiltVersion > completedVersion, "完整索引发布必须让既有父类型缓存观察到新代次")
        val refreshedParents = supertypeProvider.getDirectSupertypeDescriptors(receiverType)
        assertNotSame(parents, refreshedParents)
        assertEquals(parents, refreshedParents)
        assertSame(refreshedParents, supertypeProvider.getDirectSupertypeDescriptors(receiverType))
        assertEquals(rebuiltVersion, sourceProvider.modificationCount)
    }

    /** 两份 regular 源码来源触发 combined 查询；可见性和有界扩展仍按各使用文件判定。 */
    @Test
    fun regularSourceExtendsRespectImportsVisibilityAndBounds(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val provider = testServices.cjTestModuleStructure.getModule("provider")
        val auxiliary = testServices.cjTestModuleStructure.getModule("auxiliary")
        val caller = assertNoErrors(mainFile)

        val visibleValue = assertExtensionCall(
            caller, "useVisiblePrivateImport", "visibleValue", provider, "declarations.cj",
        )
        assertExtensionCall(caller, "useOtherDependency", "auxiliaryValue", auxiliary, "auxiliary.cj")
        assertExtensionCall(caller, "useBoundedExtension", "boundedValue", provider, "declarations.cj")

        val auxiliaryFile = sourceFile(auxiliary, "auxiliary.cj")
        val auxiliaryHelper = auxiliaryFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertSame(auxiliaryHelper.symbol, resolvedFunctionCall(caller, "useAuxiliaryHelper", "auxiliaryHelper"))

        val publicConsumer = testServices.cjTestModuleStructure.getModule("publicConsumer")
        val publicImportCaller = assertNoErrors(publicConsumer.cjFiles.single())
        assertSame(
            visibleValue,
            assertExtensionCall(publicImportCaller, "useVisiblePublicImport", "visibleValue", provider, "declarations.cj"),
        )

        assertNotMember(mainModule.cjFiles.single { it.name == "privateMember.cj" }, "privateValue")
        assertNotMember(mainModule.cjFiles.single { it.name == "missingBoundImport.cj" }, "boundedValue")
        assertNotMember(mainModule.cjFiles.single { it.name == "unsatisfiedBound.cj" }, "boundedValue")

        // 成员访问失败不能丢弃整个源码扩展，结构性 owner 查询仍须包含 private 声明。
        val receiverExtend = checkNotNull(visibleValue.getContainingExtend())
        val privateValue = receiverExtend.declarations.filterIsInstance<CfirNamedFunction>()
            .single { it.name.asString() == "privateValue" }
        assertSame(receiverExtend, caller.moduleData.session.extendProvider.getContainingExtend(privateValue.symbol))
    }

    /** 首次请求先进入真实 dangling 模块，同时看到 context 自身及其 friend 来源的扩展。 */
    @Test
    fun friendAndDanglingContextExtendsKeepSourceOwners(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val friend = testServices.cjTestModuleStructure.getModule("friendSource")
        val copiedFile = CjPsiFactory.contextual(mainFile, markGenerated = true)
            .createFile("dangling.cj", mainFile.text)
            .apply { originalFile = mainFile }

        val danglingCaller = assertNoErrors(copiedFile)
        val danglingModule = danglingCaller.llCfirModuleData.caModule as CaDanglingFileModuleImpl
        assertSame(mainModule.caModule, danglingModule.contextModule)

        val contextValue = assertExtensionCall(
            danglingCaller, "useContextExtension", "contextValue", mainModule, "context.cj",
        )
        val friendValue = assertExtensionCall(danglingCaller, "useFriendExtension", "friendValue", friend, "friend.cj")

        val contextCaller = assertNoErrors(mainFile)
        assertSame(
            contextValue,
            assertExtensionCall(contextCaller, "useContextExtension", "contextValue", mainModule, "context.cj"),
        )
        assertSame(
            friendValue,
            assertExtensionCall(contextCaller, "useFriendExtension", "friendValue", friend, "friend.cj"),
        )
    }

    /** 重复 regular 边和菱形 refinement 路径必须指向同一份源码扩展与成员符号。 */
    @Test
    fun duplicateAndDiamondDependenciesKeepOneExtendSymbol(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val ancestor = testServices.cjTestModuleStructure.getModule("ancestor")
        // 在任何 LL 请求前安装重复平台依赖边；不替换 session，也不修改已绑定的 module data。
        (mainModule.caModule as CaMutableTestModule).directRegularDependencies += ancestor.caModule

        val caller = assertNoErrors(mainFile)
        val firstSymbol = assertExtensionCall(caller, "useSharedExtension", "sharedValue", ancestor, "ancestor.cj")
        val secondSymbol = assertExtensionCall(caller, "useSharedExtensionAgain", "sharedValue", ancestor, "ancestor.cj")
        assertSame(firstSymbol, secondSymbol)

        val extend = assertSingleSourceExtend(caller, ancestor, "ancestor.cj")
        assertSame(firstSymbol, extend.declarations.filterIsInstance<CfirNamedFunction>().single().symbol)
    }

    /** 两个源码模块互为 regular 依赖时，双向调用和扩展查询必须终止并保留各自来源。 */
    @Test
    fun cyclicRegularDependenciesResolveSourceExtends(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val moduleB = testServices.cjTestModuleStructure.getModule("cycleB")
        // 夹具已有 B -> A；这里只安装 A -> B 后向边，仍使用真实源码模块及其默认 LL session。
        (mainModule.caModule as CaMutableTestModule).directRegularDependencies += moduleB.caModule

        val callerA = assertNoErrors(mainFile)
        assertExtensionCall(callerA, "useExtensionFromB", "valueFromB", moduleB, "receiverB.cj")
        val valueFromA = assertExtensionCall(callerA, "useOwnExtensionFromA", "valueFromA", mainModule, "receiverA.cj")

        val callerB = assertNoErrors(moduleB.cjFiles.single { it.name == "callerB.cj" })
        assertSame(
            valueFromA,
            assertExtensionCall(callerB, "useExtensionFromA", "valueFromA", mainModule, "receiverA.cj"),
        )
        assertSingleSourceExtend(callerA, mainModule, "receiverA.cj")
        assertSingleSourceExtend(callerA, moduleB, "receiverB.cj")
        assertSingleSourceExtend(callerB, mainModule, "receiverA.cj")
    }

    /** 首个语义操作只收集调用文件诊断；来源文件的 CFIR 查询全部留到成功解析之后。 */
    private fun assertNoErrors(file: CjFile): CfirFile {
        val errors = collectErrors(file)
        assertEquals(emptyList<String>(), errors.map { it.factoryName }, errors.joinToString { (it as CjDiagnostic).renderMessage() })
        return file.getOrBuildCfirFile(file.getResolutionFacadeForTest())
    }

    /** 官方负例可能伴随 unused import 警告；本组只断言扩展访问错误及其文件归属。 */
    private fun collectErrors(file: CjFile): List<CjPsiDiagnostic> {
        val diagnostics = file.collectDiagnosticsForFile(
            file.getResolutionFacadeForTest(),
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
        )
        assertTrue(
            diagnostics.all { it.psiElement.containingFile === file },
            "调用者诊断不能混入依赖文件：${diagnostics.map { it.psiElement.containingFile?.name to it.factoryName }}",
        )
        return diagnostics.filter { it.severity == Severity.ERROR }
    }

    /** 三个官方负例都在成员名称处报告 sema_not_member_of，对应本仓 NOT_MEMBER_OF。 */
    private fun assertNotMember(file: CjFile, memberName: String) {
        val diagnostics = collectErrors(file)
        assertEquals(listOf(CfirErrors.NOT_MEMBER_OF.name), diagnostics.map { it.factoryName })
        assertEquals(
            listOf(memberName),
            diagnostics.single().textRanges.map { range -> file.text.substring(range.startOffset, range.endOffset) },
        )
    }

    /** 比较调用的原始源码声明，并同时核验 owner 与 consumer 两侧的 extend/file 元数据。 */
    private fun assertExtensionCall(
        caller: CfirFile,
        callerName: String,
        memberName: String,
        ownerModule: CjTestModule,
        ownerFileName: String,
    ): CfirNamedFunctionSymbol {
        val target = resolvedFunctionCall(caller, callerName, memberName)
        val ownerFile = sourceFile(ownerModule, ownerFileName)
        val expectedExtend = ownerFile.declarations.filterIsInstance<CfirExtend>().single { extend ->
            extend.declarations.filterIsInstance<CfirNamedFunction>().any { it.name.asString() == memberName }
        }
        val expectedMember = expectedExtend.declarations.filterIsInstance<CfirNamedFunction>()
            .single { it.name.asString() == memberName }

        // 有界扩展的调用可以携带类型替换外壳；元数据 owner 必须仍是原始声明。
        assertSame(expectedMember, target.getDeclarationMetadataOwner())
        assertSame(expectedExtend, target.getContainingExtend())
        assertSame(ownerFile, target.getContainingFile())
        assertSame(ownerFile, expectedExtend.getContainingFile())
        assertSame(ownerModule.caModule, expectedExtend.llCfirModuleData.caModule)

        val consumerProvider = caller.moduleData.session.extendProvider
        assertSame(expectedExtend, consumerProvider.getContainingExtend(expectedMember.symbol))
        assertSame(ownerFile, consumerProvider.getContainingFile(expectedExtend))
        return expectedMember.symbol
    }

    /** 查询图夹具中目标类型的扩展，检查唯一结果的符号确为同一份源码身份。 */
    private fun assertSingleSourceExtend(caller: CfirFile, ownerModule: CjTestModule, ownerFileName: String): CfirExtend {
        val ownerFile = sourceFile(ownerModule, ownerFileName)
        val receiver = ownerFile.declarations.filterIsInstance<CfirClass>().single()
        val expectedExtend = ownerFile.declarations.filterIsInstance<CfirExtend>().single()
        val extends = caller.moduleData.session.extendProvider.getExtendsForClass(receiver.symbol.classId)
        assertEquals(1, extends.size)
        assertSame(expectedExtend.symbol, extends.single().symbol)
        assertSame(ownerFile, extends.single().getContainingFile())
        assertSame(ownerModule.caModule, extends.single().llCfirModuleData.caModule)
        return extends.single()
    }

    /** 此入口仅由首次调用者诊断之后的归属断言使用，不承担扩展索引预热。 */
    private fun sourceFile(module: CjTestModule, fileName: String): CfirFile {
        val file = module.cjFiles.single { it.name == fileName }
        return file.getOrBuildCfirFile(file.getResolutionFacadeForTest())
    }

    /** 从已经完成诊断解析的调用者正文中按实际成员名称取得调用目标。 */
    private fun resolvedFunctionCall(file: CfirFile, functionName: String, memberName: String): CfirNamedFunctionSymbol {
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == functionName }
        val calls = mutableListOf<CfirFunctionCall>()
        requireNotNull(function.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                functionCall.acceptChildren(this)
            }
        })
        val call = calls.single { call ->
            val reference = call.calleeReference
            reference is CfirNamedReference && reference.name.asString() == memberName
        }
        return (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol as CfirNamedFunctionSymbol
    }
}

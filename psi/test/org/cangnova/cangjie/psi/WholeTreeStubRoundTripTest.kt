package org.cangnova.cangjie.psi

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.ObjectStubBase
import com.intellij.psi.stubs.ObjectStubTree
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.SerializationManagerEx
import com.intellij.psi.stubs.SerializationManagerImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubElementTypeHolderEP
import com.intellij.psi.stubs.StubIndexExtension
import com.intellij.psi.stubs.StubTree
import com.intellij.psi.stubs.StubTreeLoader
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.psi.psiUtil.isPropertyParameter
import org.cangnova.cangjie.psi.stubs.CangJieClassStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 真实整树协议与物理文件正常 attach 分别验证，禁止借源 AST 或手工 PSI 关联回答查询。 */
class WholeTreeStubRoundTripTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @TempDir
    lateinit var directory: Path

    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun testProductionWholeTreeRoundTripAndPhysicalLoaderAttach() = assertRoundTrip(SOURCE, false)

    @Test
    fun invalidDeclarationRoundTripsWithoutSignatureOrAstLoading() = assertRoundTrip(
        SOURCE.replace("public init(value: Int64) {}", "OwneX<T>(public let leaked: T) where T <: Bound { func hidden(): Unit {} }\n    public init(value: Int64) {}"),
        true,
    )

    private fun assertRoundTrip(source: String, hasInvalidDeclaration: Boolean) {
        val originalFile = createPsiFile("wholeTreeOriginal", source) as CjFile
        if (hasInvalidDeclaration) assertEquals(1, originalFile.collectParsingDiagnostics().size)
        else assertEquals(emptyList(), originalFile.collectParsingDiagnostics(), "合法基线必须由真实 parser 无错误解析")
        val original = CjFileStubBuilder().buildStubTree(originalFile) as CangJieFileStub
        val originalShape = treeShape(original)
        val originalKind = original.kind
        val originalClass = descendants(original).filterIsInstance<CangJieClassStub>().single()
        assertEquals("whole_tree.Owner", originalClass.getClassId()?.asSingleFqName()?.asString())
        val originalMember = descendants(original).filterIsInstance<CangJieNamedFunctionStub>()
            .single { it.name == "member" }
        kotlin.test.assertEquals("whole_tree.Owner.member", originalMember.getFqName()?.asString(), "先验证 producer，没有空名字进入序列化")

        val application = ApplicationManager.getApplication() as MockApplication
        CoreApplicationEnvironment.registerExtensionPoint(
            application.extensionArea, StubElementTypeHolderEP.EP_NAME, StubElementTypeHolderEP::class.java,
        )
        CoreApplicationEnvironment.registerExtensionPoint(
            application.extensionArea, StubIndexExtension.EP_NAME, StubIndexExtension::class.java,
        )
        val previousManager = application.getServiceIfCreated(SerializationManagerEx::class.java)
        val manager = SerializationManagerImpl(Supplier<Path?> { null }, false, application.getCoroutineScope())
        application.picoContainer.unregisterComponent(SerializationManagerEx::class.java.name)
        application.registerService(SerializationManagerEx::class.java, manager, testRootDisposable)
        manager.initialize()
        try {
            val bytes = ByteArrayOutputStream().also { manager.serialize(original, it) }.toByteArray()
            val detached = deserialize(manager, bytes)
            assertEquals(originalShape, treeShape(detached), "整树协议保存类型、顺序、parent、字段和 dangling")
            assertEquals(originalKind, detached.kind)
            assertRootMetadata(detached)
            assertNull(detached.psi, "detached 根不得保留原文件 PSI")
            assertDeclarationFacts(detached, hasInvalidDeclaration) { label, query ->
                assertNull(detached.psi, "查询前 detached 根没有 PSI：$label")
                query()
                assertNull(detached.psi, "查询后 detached 根仍没有 PSI：$label")
            }

            // attach 使用另一棵树，不复用已经创建 child PSI 的 detached 树。
            val attachRoot = deserialize(manager, bytes)
            assertNotSame(detached, attachRoot)
            assertNull(attachRoot.psi)
            assertEquals(originalShape, treeShape(attachRoot))
            assertRootMetadata(attachRoot)
            val path = directory.resolve("wholeTreeFresh.cj")
            Files.writeString(path, source, Charsets.UTF_8)
            val fileSystem = VirtualFileManager.getInstance().getFileSystem("file")
            assertTrue(fileSystem is CoreLocalFileSystem, "必须使用 core 的真实本地文件系统")
            val virtualFile = kotlin.test.assertNotNull(fileSystem.findFileByNioFile(path))
            assertEquals(source, virtualFile.contentsToByteArray().toString(Charsets.UTF_8))
            val fresh = kotlin.test.assertNotNull(PsiManager.getInstance(project).findFile(virtualFile)) as CjFile
            assertNotSame(originalFile, fresh)
            assertNotSame(originalFile.virtualFile, fresh.virtualFile)
            assertTrue(fresh.viewProvider.isPhysical, "LightVirtualFile 或非物理 provider 不能证明正常 attach")
            val fileImpl = fresh as PsiFileImpl
            assertFalse(fileImpl.isContentsLoaded)

            val previousLoader = StubTreeLoader.getInstance()
            val loader = StrictRoundTripLoader(project, virtualFile, fresh, StubTree(attachRoot))
            application.picoContainer.unregisterComponent(StubTreeLoader::class.java.name)
            application.registerService(StubTreeLoader::class.java, loader)
            try {
                assertSame(attachRoot, kotlin.test.assertNotNull(fresh.stubTree).root)
                assertEquals(1, loader.readCount)
                assertSame(fresh, attachRoot.psi, "根 PSI 由 PsiFileImpl 的正常 loader attach 安装")
                assertFalse(fileImpl.isContentsLoaded)
                checkedWithoutAst(fileImpl, "file declarations") {
                    assertEquals(listOf("Owner", "nativeCall", "ordinary"), fresh.declarations.map { (it as CjNamedDeclaration).name })
                }
                assertDeclarationFacts(attachRoot, hasInvalidDeclaration) { label, query -> checkedWithoutAst(fileImpl, label, query) }
                assertEquals(1, loader.readCount, "后续 getters 使用已安装树，不重复读取或构建 AST")
                assertSame(attachRoot, kotlin.test.assertNotNull(fresh.stubTree).root)
                assertFalse(fileImpl.isContentsLoaded)
            } finally {
                application.picoContainer.unregisterComponent(StubTreeLoader::class.java.name)
                application.registerService(StubTreeLoader::class.java, previousLoader)
            }
        } finally {
            application.picoContainer.unregisterComponent(SerializationManagerEx::class.java.name)
            if (previousManager != null) application.registerService(SerializationManagerEx::class.java, previousManager)
        }
    }

    /** 每项 getter 单独检查 AST 状态，定位任何隐式加载源文件的查询。 */
    private fun checkedWithoutAst(file: PsiFileImpl, label: String, query: () -> Unit) {
        assertFalse(file.isContentsLoaded, "查询前 AST 未加载：$label")
        query()
        assertFalse(file.isContentsLoaded, "查询后 AST 未加载：$label")
    }

    private fun assertDeclarationFacts(root: CangJieFileStub, hasInvalidDeclaration: Boolean, check: (String, () -> Unit) -> Unit) {
        val owner = descendants(root).filterIsInstance<CangJieClassStub>().single().psi
        check("class identity") {
            assertEquals("Owner", owner.name)
            assertEquals("whole_tree.Owner", owner.fqName?.asString())
            assertEquals("whole_tree.Owner", owner.getClassId()?.asSingleFqName()?.asString())
        }
        lateinit var body: CjAbstractClassBody
        check("class body declarations") {
            body = kotlin.test.assertNotNull(owner.body)
            assertEquals(
                listOf(CjPrimaryConstructor::class) +
                    (if (hasInvalidDeclaration) listOf(CjInvalidDeclaration::class) else emptyList()) +
                    listOf(CjSecondaryConstructor::class, CjFinalizer::class, CjNamedFunction::class),
                body.declarations.map { it::class },
            )
        }
        if (hasInvalidDeclaration) check("typed invalid declaration without persisted children") {
            val invalid = body.declarations.filterIsInstance<CjInvalidDeclaration>().single()
            val stub = descendants(root).single { it.psi === invalid }
            assertTrue(stub.stubType == org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.INVALID_DECLARATION)
            assertTrue(stub.childrenStubs.isEmpty(), "错误签名、参数、泛型和内部函数不得进入 stub 子树")
            assertSame(body, invalid.parent)
            assertFalse(descendants(root).filterIsInstance<CangJieNamedFunctionStub>().any { it.name == "hidden" })
        }
        lateinit var primary: CjPrimaryConstructor
        check("constructor owners and typed getters") {
            primary = body.primaryConstructors.single()
            assertSame(owner, primary.getContainingTypeStatement())
            assertSame(owner, body.secondaryConstructors.single().getContainingTypeStatement())
            assertSame(owner, body.finalizers.single().getContainingTypeStatement())
            assertFalse(primary.isLocal)
            assertTrue(primary.hasBody())
            assertTrue(body.secondaryConstructors.single().hasBody())
            assertTrue(body.finalizers.single().hasBody())
        }
        check("primary parameter order and owners") {
            val parameters = primary.valueParameters
            assertEquals(listOf("ordinary", "immutable", "mutable"), parameters.map { it.name })
            assertEquals(listOf(false, true, true), parameters.map { it.hasLetOrVar() })
            assertEquals(listOf(false, true, true), parameters.map { it.isPropertyParameter() })
            assertEquals(listOf(false, false, true), parameters.map { it.hasDefaultValue() })
            assertEquals(listOf(false, false, true), parameters.map { it.isNamed })
            parameters.forEach { parameter ->
                assertSame(primary.valueParameterList, parameter.parent)
                assertSame(primary, parameter.ownerFunction)
                assertSame(primary, parameter.ownerDeclaration)
            }
        }
        // 主构造 let/var 参数按所属类字段持久化 FQName，普通参数不获得成员名。
        check("primary immutable parameter FQName") {
            assertEquals("whole_tree.Owner.immutable", primary.valueParameters[1].fqName?.asString())
        }
        check("primary ordinary parameter FQName") {
            assertNull(primary.valueParameters[0].fqName)
        }
        check("primary mutable parameter FQName") {
            assertEquals("whole_tree.Owner.mutable", primary.valueParameters[2].fqName?.asString())
        }
        check("function local top-level FQName and body facts") {
            val member = body.declarations.filterIsInstance<CjNamedFunction>().single()
            assertFalse(member.isLocal)
            assertFalse(member.isTopLevel)
            assertEquals("whole_tree.Owner.member", member.fqName?.asString())
            assertTrue(member.hasEmptyBlockBody())
            val functions = descendants(root).map { it.psi }.filterIsInstance<CjNamedFunction>()
                .filter { it.name != "member" }.toList()
            assertEquals(listOf("nativeCall", "ordinary"), functions.map { it.name })
            functions.forEach {
                assertFalse(it.isLocal)
                assertTrue(it.isTopLevel)
                assertEquals("whole_tree.${it.name}", it.fqName?.asString())
            }
            assertFalse(functions.first().hasBody())
            assertTrue(functions.last().hasEmptyBlockBody())
            val foreignBody = functions.first().parent as CjForeignBody
            assertEquals(listOf(functions.first()), foreignBody.declarations)
        }
    }

    private fun deserialize(manager: SerializationManagerImpl, bytes: ByteArray): CangJieFileStub =
        ByteArrayInputStream(bytes).use { input ->
            (manager.deserialize(input) as CangJieFileStub).also {
                assertEquals(-1, input.read(), "整树反序列化必须消费完整协议")
            }
        }

    private fun assertRootMetadata(root: CangJieFileStub) {
        val fileStub = root as PsiFileStubImpl<*>
        assertTrue(fileStub.rootsAreSet(), "生产反序列化必须安装 roots 元信息")
        assertEquals(1, fileStub.stubRoots.size)
        assertSame(root, fileStub.stubRoots.single())
        assertNull(root.parentStub)
    }

    /** 遍历的是 stub 子树；字段描述来自真实 stub，绝不逐节点重建序列化协议。 */
    private fun treeShape(root: StubElement<*>): List<String> {
        val nodes = listOf(root) + descendants(root).toList()
        return nodes.mapIndexed { index, stub ->
            val parentIndex = stub.parentStub?.let { parent -> nodes.indexOfFirst { it === parent } } ?: -1
            "$index:$parentIndex:${stub.stubType}:${(stub as ObjectStubBase<*>).isDangling}:$stub"
        }
    }

    private fun descendants(root: StubElement<*>): Sequence<StubElement<*>> = sequence {
        for (child in root.childrenStubs) {
            yield(child)
            yieldAll(descendants(child))
        }
    }

    /** 只允许正常 readOrBuild 关联指定物理文件；任何构建或其他文件读取都是测试失败。 */
    private class StrictRoundTripLoader(
        private val expectedProject: Project,
        private val expectedFile: VirtualFile,
        private val expectedPsi: PsiFile,
        private val tree: StubTree,
    ) : StubTreeLoader() {
        var readCount: Int = 0
            private set

        override fun readOrBuild(project: Project, vFile: VirtualFile, psiFile: PsiFile?): ObjectStubTree<*> {
            assertSame(expectedProject, project)
            assertSame(expectedFile, vFile)
            assertSame(expectedPsi, psiFile)
            readCount++
            assertEquals(1, readCount)
            return tree
        }

        override fun build(project: Project?, vFile: VirtualFile, psiFile: PsiFile?): ObjectStubTree<*> =
            error("fresh attach 不得构建 AST")

        override fun readFromVFile(project: Project, vFile: VirtualFile): ObjectStubTree<*> =
            error("fresh attach 必须使用 readOrBuild")

        override fun rebuildStubTree(vFile: VirtualFile): Unit = error("不得重建 stub")

        override fun canHaveStub(file: VirtualFile): Boolean {
            assertSame(expectedFile, file)
            return true
        }
    }

    companion object {
        // let/var 主构造参数采用已验证 PrimaryConstructorMemberFieldRawCfirTest 的官方语法。
        // foreign 块采用 ForeignAndAnnotationParsingTest 的原生函数声明语法。
        private val SOURCE = """
            package whole_tree

            class Owner {
                public Owner(ordinary: Bool, public let immutable: Int64, public var mutable!: Int64 = 1) {}
                public init(value: Int64) {}
                ~init() {}
                func member(): Unit {}
            }
            foreign {
                func nativeCall(value: CPointer<Int32>): Int32
            }
            func ordinary(): Unit {}
        """.trimIndent()
    }
}

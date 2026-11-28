package org.cangnova.cangjie.psi

import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.CjmpFileRoleMode
import org.cangnova.cangjie.CjmpRoleEventOutcome
import org.cangnova.cangjie.CjmpRoleSyntaxIdentity
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ThreeState
import com.intellij.util.AstLoadingFilter
import java.awt.EventQueue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CangJieParsingDataComparator
import org.cangnova.cangjie.parsing.parsedRoleEvents
import org.cangnova.cangjie.parsing.parsedRoleSnapshot
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.CjmpFileRoleSyntaxSnapshot
import org.cangnova.cangjie.parsing.CjmpParsedRoleEvent
import org.cangnova.cangjie.parsing.CjmpParsedRoleEventId
import java.util.UUID
import org.cangnova.cangjie.parsing.PARSED_FILE_RESULT_KEY
import org.cangnova.cangjie.parsing.CjmpRoleSnapshotReplay
import org.cangnova.cangjie.parsing.cjmpDeclarationRoleEventsIn
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.CangJieStubElement
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.stubs.elements.CjFileElementType
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService
import org.cangnova.cangjie.psi.stubs.elements.CjmpFileRoleSnapshotCodec
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.psi.stubs.cjmpRoleEventsFor
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.StubElement
import com.intellij.lang.ASTNode
import java.util.IdentityHashMap
import com.intellij.util.io.AbstractStringEnumerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

/** 检查真实文件 stub 构建是否在无预先访问声明的情况下完成角色事实发布。 */
class CjmpRoleFileStubTimingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    private val fixtureRootIdentity: UUID = UUID.randomUUID()
    private fun eventId(sequence: Long) = CjmpParsedRoleEventId(fixtureRootIdentity, sequence)
    private fun event(sequence: Long, offset: Int, kind: CjmpRoleEventKind) = CjmpParsedRoleEvent(eventId(sequence), offset, kind)

    @Test
    fun stubBuildPublishesFileRoleEvents() {
        val source = "common class Owner { specific func member(): Unit {} }"
        val file = createPsiFile("roleStubTiming", source) as CjFile
        val stub = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val events = requireNotNull(file.parsedRoleEvents).filterNot { it.kind == CjmpRoleEventKind.Unknown }
        assertEquals(listOf(source.indexOf("class"), source.indexOf("func")), events.map { it.declarationOffset })
        assertEquals(listOf(
            CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
            CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ), events.map { it.kind })
        assertEquals(file.parsedRoleSnapshot?.events, stub.cjmpRoleSnapshot?.events)
    }

    @Test
    fun fileStubCreationHasPublishedRoleEvents() {
        val source = "common func first(): Unit {}"
        val file = createPsiFile("roleFileStub", source) as CjFile
        assertFalse(file.isContentsLoaded)
        val callback = CjFileStubBuilder::class.java.getDeclaredMethod("createStubForFile", PsiFile::class.java)
        callback.isAccessible = true
        callback.invoke(CjFileStubBuilder(), file)
        val events = requireNotNull(file.parsedRoleEvents)
        assertEquals(CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)), events.first().kind)
        val snapshot = requireNotNull(file.parsedRoleSnapshot)
        assertEquals(CjmpParsingContext.SyntaxOnly, snapshot.parsingContext)
        assertEquals(CjmpFileRole.NONE, snapshot.entryFileRole)
        assertEquals(CjmpRoleInputCompleteness.PARTIAL, snapshot.completeness)
    }

    @Test
    fun viewProviderContentsCanBeReadWithoutLoadingPsiTree() {
        val source = "common func first(): Unit {}"
        val file = createPsiFile("roleContentsWithoutAst", source) as CjFile
        assertFalse(file.isContentsLoaded)
        AstLoadingFilter.disallowTreeLoading<Unit, Nothing> {
            assertEquals(source, file.viewProvider.contents.toString())
            assertFalse(file.isContentsLoaded)
        }
    }

    @Test
    fun sourceFingerprintDistinguishesUtf16UnitsAndLineEndings() {
        val source = "common func first(): Unit {}\r\n// \uD800"
        val file = createPsiFile("roleFingerprintUnits", source) as CjFile
        file.node.firstChildNode
        val snapshot = requireNotNull(file.parsedRoleSnapshot)
        assertEquals(true, snapshot.matchesSourceContent(source))
        assertEquals(false, snapshot.matchesSourceContent(source.replace('\uD800', '\uD801')))
        assertEquals(false, snapshot.matchesSourceContent(source.replace("\r\n", "\n")))
        val unknown = CjmpFileRoleSyntaxSnapshot(CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE,
            CjmpRoleInputCompleteness.PARTIAL, emptyList())
        assertNull(unknown.matchesSourceContent(source))
    }

    @Test
    fun copiedParseResultFingerprintTracksItsSourceText() {
        val source = "common func first(): Unit {}"
        val file = createPsiFile("roleFingerprintCopy", source) as CjFile
        val packageNode = requireNotNull(file.node.findChildByType(CjNodeTypes.PACKAGE_DIRECTIVE))
        val parsed = requireNotNull(packageNode.getCopyableUserData(PARSED_FILE_RESULT_KEY))
        val changed = parsed.copy(sourceText = source + " ")
        assertNotEquals(parsed.sourceFingerprint, changed.sourceFingerprint)
        assertEquals(parsed.sourceFingerprint, parsed.copy(roleEvents = parsed.roleEvents.toList()).sourceFingerprint)
    }

    @Test
    fun viewProviderContentsTrackChangedLightVirtualFileDocument() {
        val file = createPsiFile("roleDocumentContents", "common func first(): Unit {}") as CjFile
        file.node.firstChildNode
        val snapshot = requireNotNull(file.parsedRoleSnapshot)
        val document = requireNotNull(PsiDocumentManager.getInstance(project).getDocument(file))
        val updated = "specific func second(): Unit {}"
        EventQueue.invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction { document.setText(updated) }
            }, "更新文档", null)
        }
        assertEquals(updated, document.text)
        assertEquals(updated, file.viewProvider.contents.toString())
        assertEquals(false, snapshot.matchesSourceContent(file.viewProvider.contents))
    }

    @Test
    fun fileStubNeverPublishesRoleSnapshotForDifferentDocumentContent() {
        val file = createPsiFile("roleChangedStub", "common func first(): Unit {}") as CjFile
        val initial = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        assertEquals(true, requireNotNull(initial.cjmpRoleSnapshot).matchesSourceContent(file.viewProvider.contents))
        val document = requireNotNull(PsiDocumentManager.getInstance(project).getDocument(file))
        EventQueue.invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction { document.setText("specific func second(): Unit {}") }
            }, "更新文档", null)
        }
        val rebuilt = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val snapshot = rebuilt.cjmpRoleSnapshot
        assertTrue(snapshot == null || snapshot.matchesSourceContent(file.viewProvider.contents) == true)
    }

    @Test
    fun viewProviderStampChangesWhenDocumentReturnsToOriginalText() {
        val original = "common func first(): Unit {}"
        val file = createPsiFile("roleStampGeneration", original) as CjFile
        val document = requireNotNull(PsiDocumentManager.getInstance(project).getDocument(file))
        val before = file.viewProvider.modificationStamp
        fun replace(text: String) = EventQueue.invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction { document.setText(text) }
            }, "更新文档", null)
        }
        replace("specific func second(): Unit {}")
        val changed = file.viewProvider.modificationStamp
        replace(original)
        val restored = file.viewProvider.modificationStamp
        assertEquals(original, file.viewProvider.contents.toString())
        assertNotEquals(before, changed)
        assertNotEquals(before, restored)
        assertNotEquals(changed, restored)
    }

    @Test
    fun sdkReparseReturningToOriginalTextCannotReuseOldRoleEvents() {
        val original = "common func first(): Unit {}"
        val file = createPsiFile("roleCommittedGeneration", original) as CjFile
        val originalSnapshot = requireNotNull((CjFileStubBuilder().buildStubTree(file) as CangJieFileStub).cjmpRoleSnapshot)
        val originalEvents = originalSnapshot.events.toList()
        assertTrue(originalEvents.isNotEmpty())
        fun reparse(oldText: String, newText: String) {
            val result = BlockSupportImpl.reparse(
                file, file.node, TextRange(0, oldText.length), newText, EmptyProgressIndicator(), oldText,
            )
            EventQueue.invokeAndWait {
                CommandProcessor.getInstance().executeCommand(project, {
                    ApplicationManager.getApplication().runWriteAction { result.log.doActualPsiChange(file) }
                }, "应用解析结果", null)
            }
        }
        reparse(original, "specific func second(): Unit {}")
        assertEquals("specific func second(): Unit {}", file.text)
        reparse("specific func second(): Unit {}", original)
        assertEquals(original, file.text)
        val function = requireNotNull(PsiTreeUtil.findChildOfType(file, CjNamedFunction::class.java))
        val keyword = requireNotNull(function.node.findChildByType(CjTokens.FUNC_KEYWORD))
        assertNull(keyword.cjmpDeclarationRoleEventsIn(originalSnapshot))
        val rebuilt = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val newSnapshot = rebuilt.cjmpRoleSnapshot
        assertTrue(newSnapshot == null || newSnapshot.events.none { current -> originalEvents.any { it === current } })
        if (newSnapshot != null) {
            val functionStub = rebuilt.childrenStubs.filterIsInstance<CangJieNamedFunctionStub>().single()
            val boundEvents = rebuilt.cjmpRoleEventsFor(functionStub)
            assertTrue(boundEvents == null || boundEvents.all { bound -> newSnapshot.events.any { it === bound } })
        }
    }

    @Test
    fun sourceStubStructureAndParsedRoleAnchorsAreConsistent() {
        val source = "common class Owner { specific func member(): Unit {} }"
        val file = createPsiFile("roleStubAnchors", source) as CjFile
        val root = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val owner = root.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>().single()
        val body = owner.childrenStubs.single { it.stubType == CjStubElementTypes.CLASS_BODY }
        val member = body.childrenStubs.filterIsInstance<CangJieNamedFunctionStub>().single()
        val ownerPsi = requireNotNull(PsiTreeUtil.findChildOfType(file, CjClass::class.java))
        val memberPsi = requireNotNull(PsiTreeUtil.findChildOfType(ownerPsi, CjNamedFunction::class.java))
        assertEquals(owner.name, ownerPsi.name)
        assertEquals(member.name, memberPsi.name)
        val ownerKeyword = requireNotNull(ownerPsi.node.findChildByType(CjTokens.CLASS_KEYWORD))
        val memberKeyword = requireNotNull(memberPsi.node.findChildByType(CjTokens.FUNC_KEYWORD))
        val events = requireNotNull(root.cjmpRoleSnapshot).events
        assertEquals(source.indexOf("class"), ownerKeyword.startOffset)
        assertEquals(source.indexOf("func"), memberKeyword.startOffset)
        assertEquals(listOf(ownerKeyword.startOffset, memberKeyword.startOffset),
            events.filter { it.kind is CjmpRoleEventKind.Check }.map { it.declarationOffset })
        assertEquals(ownerKeyword.startOffset, requireNotNull(root.cjmpRoleEventsFor(owner)).single().declarationOffset)
        assertEquals(memberKeyword.startOffset, requireNotNull(root.cjmpRoleEventsFor(member)).single().declarationOffset)
        val copied = (root as CangJieFileStubImpl).deepCopy()
        val copiedOwner = copied.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>().single()
        val copiedMember = copiedOwner.childrenStubs.single { it.stubType == CjStubElementTypes.CLASS_BODY }
            .childrenStubs.filterIsInstance<CangJieNamedFunctionStub>().single()
        assertEquals(ownerKeyword.startOffset, requireNotNull(copied.cjmpRoleEventsFor(copiedOwner)).single().declarationOffset)
        assertEquals(memberKeyword.startOffset, requireNotNull(copied.cjmpRoleEventsFor(copiedMember)).single().declarationOffset)
        assertNull(copied.cjmpRoleEventsFor(owner))
        val strings = TestStrings()
        val bytes = ByteArrayOutputStream()
        StubOutputStream(bytes, strings).use { CjFileElementType.INSTANCE.serialize(root, it) }
        val restored = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use {
            CjFileElementType.INSTANCE.deserialize(it, null)
        }
        assertEquals(true, requireNotNull(restored.cjmpRoleSnapshot).matchesSourceContent(source))
        assertEquals(false, requireNotNull(restored.cjmpRoleSnapshot).matchesSourceContent(source + " "))
        fun copyChildren(sourceStub: StubElement<*>, targetStub: StubElement<*>) {
            for (child in sourceStub.childrenStubs) {
                val copy = (child as CangJieStubElement<*>).copyInto(targetStub)
                copyChildren(child, copy)
            }
        }
        copyChildren(root, restored)
        val restoredOwner = restored.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>().single()
        val restoredMember = restoredOwner.childrenStubs.single { it.stubType == CjStubElementTypes.CLASS_BODY }
            .childrenStubs.filterIsInstance<CangJieNamedFunctionStub>().single()
        val ownerOffset = ownerKeyword.startOffset
        val memberOffset = memberKeyword.startOffset
        AstLoadingFilter.disallowTreeLoading<Unit, Nothing> {
            assertEquals(ownerOffset, requireNotNull(restored.cjmpRoleEventsFor(restoredOwner)).single().declarationOffset)
            val memberEvents = requireNotNull(restored.cjmpRoleEventsFor(restoredMember))
            assertEquals(memberOffset, memberEvents.single().declarationOffset)
            val replay = CjmpRoleSnapshotReplay.replay(requireNotNull(restored.cjmpRoleSnapshot),
                CjmpFileRoleMode.Compilation(true, true, false, false), LanguageVersionSettingsImpl.DEFAULT,
                CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT)
            assertEquals(CjmpRoleEventOutcome.REJECTED, requireNotNull(replay.resultsFor(memberEvents)).single().outcome)
            assertNull(restored.cjmpRoleEventsFor(owner))
        }
    }

    @Test
    fun sourceStubBindingsCoverDifferentDeclarationKinds() {
        val source = "common class Owner {}\ncommon struct Record {}\ncommon extend Int64 {}\ncommon func top(): Unit {}"
        val file = createPsiFile("roleStubKinds", source) as CjFile
        val root = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val snapshot = requireNotNull(root.cjmpRoleSnapshot)
        val eventsById = snapshot.events.associateBy { it.id }
        val boundOffsets = snapshot.stubBindings.values.flatten().map { requireNotNull(eventsById[it]).declarationOffset }.toSet()
        assertEquals(listOf("class", "struct", "extend", "func").map { source.indexOf(it) }.toSet(), boundOffsets)
    }

    @Test
    fun replayResultsRequireEventsFromTheSameSnapshot() {
        val source = "common class Owner { specific func member(): Unit {} }"
        val file = createPsiFile("roleReplayIdentity", source) as CjFile
        val root = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val owner = root.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>().single()
        val member = owner.childrenStubs.single { it.stubType == CjStubElementTypes.CLASS_BODY }
            .childrenStubs.filterIsInstance<CangJieNamedFunctionStub>().single()
        val snapshot = requireNotNull(root.cjmpRoleSnapshot)
        val events = requireNotNull(root.cjmpRoleEventsFor(member))
        val mode = CjmpFileRoleMode.Compilation(true, true, false, false)
        val verified = CjmpRoleSnapshotReplay.replay(snapshot, mode, LanguageVersionSettingsImpl.DEFAULT,
            CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT)
        assertEquals(CjmpRoleEventOutcome.REJECTED, requireNotNull(verified.resultsFor(events)).single().outcome)
        assertNull(verified.resultsFor(events.map { it.copy() }))
        assertNull(verified.resultsFor(emptyList()))
        assertNull(verified.resultsFor(listOf(events.single(), events.single())))
        assertNull(verified.resultsFor(snapshot.events.asReversed()))
        val unknown = CjmpRoleSnapshotReplay.replay(snapshot, mode, LanguageVersionSettingsImpl.DEFAULT,
            CjmpRoleSyntaxIdentity.UNKNOWN)
        assertEquals(CjmpRoleEventOutcome.UNKNOWN, requireNotNull(unknown.resultsFor(events)).single().outcome)
    }

    @Test
    fun declarationKeywordEventsBelongToTheirParsingTransaction() {
        val source = "common class Owner { specific func member(): Unit {} }"
        val file = createPsiFile("roleEventIdentity", source) as CjFile
        val owner = requireNotNull(PsiTreeUtil.findChildOfType(file, CjClass::class.java))
        val member = requireNotNull(PsiTreeUtil.findChildOfType(owner, CjNamedFunction::class.java))
        val snapshot = requireNotNull(file.parsedRoleSnapshot)
        val ownerKeyword = requireNotNull(owner.node.findChildByType(CjTokens.CLASS_KEYWORD))
        val memberKeyword = requireNotNull(member.node.findChildByType(CjTokens.FUNC_KEYWORD))
        val ownerEvent = requireNotNull(ownerKeyword.cjmpDeclarationRoleEventsIn(snapshot)).single()
        val memberEvent = requireNotNull(memberKeyword.cjmpDeclarationRoleEventsIn(snapshot)).single()
        assertEquals(source.indexOf("class"), ownerEvent.declarationOffset)
        assertEquals(source.indexOf("func"), memberEvent.declarationOffset)

        // 同样的文本和值不能把不同解析事务的事件视为同一身份。
        val otherSnapshot = CjmpFileRoleSyntaxSnapshot(snapshot.parsingContext, snapshot.entryFileRole,
            snapshot.completeness, snapshot.events.map { it.copy() }, sourceFingerprint = snapshot.sourceFingerprint,
            lexicalSource = snapshot.lexicalSource)
        assertEquals(snapshot, otherSnapshot)
        assertNull(ownerKeyword.cjmpDeclarationRoleEventsIn(otherSnapshot))
        assertNull(memberKeyword.cjmpDeclarationRoleEventsIn(otherSnapshot))
    }

    @Test
    fun equalTextFromDistinctTransactionsCannotReuseRoleCarriers() {
        val source = "common func first(): Unit {}"
        val first = createPsiFile("firstRoleGeneration", source) as CjFile
        val second = createPsiFile("secondRoleGeneration", source) as CjFile
        val firstFunction = requireNotNull(PsiTreeUtil.findChildOfType(first, CjNamedFunction::class.java))
        val secondFunction = requireNotNull(PsiTreeUtil.findChildOfType(second, CjNamedFunction::class.java))
        val firstKeyword = requireNotNull(firstFunction.node.findChildByType(CjTokens.FUNC_KEYWORD))
        val secondKeyword = requireNotNull(secondFunction.node.findChildByType(CjTokens.FUNC_KEYWORD))
        val comparator = CangJieParsingDataComparator()
        assertEquals(ThreeState.NO, comparator.compareAST(firstKeyword, secondKeyword))
        assertEquals(ThreeState.NO, comparator.compareAST(
            requireNotNull(first.node.findChildByType(CjNodeTypes.PACKAGE_DIRECTIVE)),
            requireNotNull(second.node.findChildByType(CjNodeTypes.PACKAGE_DIRECTIVE)),
        ))
    }

    @Test
    fun stubVisitorCanCaptureSourceEventAtStubCreation() {
        val source = "common class Owner { specific func member(): Unit {} }"
        val file = createPsiFile("roleStubVisitor", source) as CjFile
        val captured = IdentityHashMap<StubElement<*>, List<CjmpParsedRoleEvent>>()
        val builder = object : DefaultStubBuilder() {
            override fun buildStubTree(file: PsiFile): StubElement<*> {
                val rootNode = file.node
                val root = createStubForFile(file)
                rootNode.firstChildNode
                val snapshot = requireNotNull((file as CjFile).parsedRoleSnapshot)
                val visitor = object : StubBuildingWalkingVisitor(rootNode, root) {
                    override fun createStub(parentStub: StubElement<*>, node: ASTNode): StubElement<*>? {
                        val stub = super.createStub(parentStub, node) ?: return null
                        val events = generateSequence(node.firstChildNode) { it.treeNext }
                            .mapNotNull { it.cjmpDeclarationRoleEventsIn(snapshot) }.flatten().toList()
                        if (events.isNotEmpty()) captured[stub] = events
                        return stub
                    }
                }
                visitor.buildStubTree()
                return root
            }
        }
        val root = builder.buildStubTree(file)
        val owner = root.childrenStubs.filterIsInstance<CangJieTypeStatementStub<*>>().single()
        val body = owner.childrenStubs.single { it.stubType == CjStubElementTypes.CLASS_BODY }
        val member = body.childrenStubs.filterIsInstance<CangJieNamedFunctionStub>().single()
        assertEquals(source.indexOf("class"), requireNotNull(captured[owner]).single().declarationOffset)
        assertEquals(source.indexOf("func"), requireNotNull(captured[member]).single().declarationOffset)
    }

    @Test
    fun fileStubCodecsAndCopyPreserveRoleSnapshot() {
        val context = CjmpParsingContext.Compilation(
            true, false, true, CjmpFileRole.COMMON, true, true, true, false, false, false, true, true,
        )
        val snapshot = CjmpFileRoleSyntaxSnapshot(context, CjmpFileRole.SPECIFIC, CjmpRoleInputCompleteness.PARTIAL, listOf(
            event(1, 3, CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false))),
            event(4, 8, CjmpRoleEventKind.Extend(CjmpRoleModifiers(false, true, false), 2)),
            event(7, 12, CjmpRoleEventKind.Skipped),
            event(9, 15, CjmpRoleEventKind.Unknown),
        ), mapOf(listOf(0, 2) to listOf(eventId(1)), listOf(1) to listOf(eventId(4))))
        val service = object : StubIndexService() {}
        val syntax = CjmpFileRoleSyntaxSnapshot(CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE,
            CjmpRoleInputCompleteness.PREFIX, emptyList())
        val complete = CjmpFileRoleSyntaxSnapshot(context, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, emptyList())
        for (value in listOf(null, snapshot, syntax, complete)) {
            val original = CangJieFileStubImpl(null, CangJieFileStubKindImpl.File(FqName.ROOT), value)
            assertEquals(value, original.copyInto(null).cjmpRoleSnapshot)
            for (useService in listOf(false, true)) {
                val strings = TestStrings()
                val bytes = ByteArrayOutputStream()
                StubOutputStream(bytes, strings).use { output ->
                    if (useService) service.serializeFileStub(original, output) else CjFileElementType.INSTANCE.serialize(original, output)
                }
                val restored = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use { input ->
                    val result = if (useService) service.deserializeFileStub(input) else CjFileElementType.INSTANCE.deserialize(input, null)
                    assertEquals(-1, input.read())
                    result
                }
                assertNull(restored.psi)
                assertEquals(value, restored.cjmpRoleSnapshot)
            }
        }
    }

    @Test
    fun unknownSnapshotVersionIsNotTreatedAsMissing() {
        val bytes = byteArrayOf(1, 127)
        StubInputStream(ByteArrayInputStream(bytes), TestStrings()).use { input ->
            assertFailsWith<IOException> { CjmpFileRoleSnapshotCodec.read(input) }
        }
    }

    @Test
    fun codecsPreserveInterleavedRootIdentitiesAndBindingOrder() {
        val first = event(4, 10, CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)))
        val second = CjmpParsedRoleEvent(CjmpParsedRoleEventId(UUID.randomUUID(), 0), 2,
            CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)))
        val third = event(5, 20, CjmpRoleEventKind.Check(CjmpRoleModifiers(false, false, false)))
        val snapshot = CjmpFileRoleSyntaxSnapshot(CjmpParsingContext.Compilation(true, true, true),
            CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(first, second, third),
            mapOf(listOf(0) to listOf(first.id, second.id), listOf(1) to listOf(third.id)))
        val bytes = ByteArrayOutputStream()
        val strings = TestStrings()
        StubOutputStream(bytes, strings).use { CjmpFileRoleSnapshotCodec.write(it, snapshot) }
        val restored = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use { input ->
            val result = requireNotNull(CjmpFileRoleSnapshotCodec.read(input))
            assertEquals(-1, input.read())
            result
        }
        assertEquals(snapshot, restored)
        assertEquals(listOf(first.id, second.id, third.id), restored.events.map { it.id })
        val replay = CjmpRoleSnapshotReplay.replay(restored, CjmpFileRoleMode.Compilation(true, true, true, false),
            LanguageVersionSettingsImpl.DEFAULT, CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT)
        assertEquals(listOf(0L, 1L, 2L), replay.result.events.map { it.event.order })
        assertEquals(listOf(CjmpRoleEventOutcome.ACCEPTED, CjmpRoleEventOutcome.REJECTED, CjmpRoleEventOutcome.ACCEPTED),
            requireNotNull(replay.resultsFor(restored.events)).map { it.outcome })
        assertNull(replay.resultsFor(listOf(first)))
        assertFailsWith<IllegalArgumentException> {
            restored.withStubBindings(mapOf(listOf(0) to listOf(second.id, first.id)))
        }
    }

    @Test
    fun stubBindingsRejectOutOfOrderOrReusedEvents() {
        val snapshot = CjmpFileRoleSyntaxSnapshot(CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE,
            CjmpRoleInputCompleteness.PARTIAL, listOf(
                event(1, 1, CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false))),
                event(2, 2, CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false))),
            ))
        assertFailsWith<IllegalArgumentException> { snapshot.withStubBindings(mapOf(listOf(0) to listOf(eventId(2), eventId(1)))) }
        assertFailsWith<IllegalArgumentException> { snapshot.withStubBindings(mapOf(listOf(0) to listOf(eventId(1)), listOf(1) to listOf(eventId(1)))) }
        assertFailsWith<IllegalArgumentException> { snapshot.withStubBindings(mapOf(listOf(0) to listOf(eventId(3)))) }
    }

    @Test
    fun localPsiEditInvalidatesOldFileRoleSnapshot() {
        val source = "common func first(): Unit {}"
        val file = createPsiFile("roleEdit", source) as CjFile
        val original = requireNotNull(file.findElementAt(source.indexOf("first")))
        requireNotNull(file.parsedRoleSnapshot)
        val replacementSource = "func other(): Unit {}"
        val replacementFile = createPsiFile("roleReplacement", replacementSource) as CjFile
        val replacement = requireNotNull(replacementFile.findElementAt(replacementSource.indexOf("other")))
        EventQueue.invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction { original.replace(replacement) }
            }, "更新声明", null)
        }
        assertEquals("common func other(): Unit {}", file.text)
        assertNull(file.parsedRoleSnapshot)
        val rebuilt = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        assertNull(rebuilt.cjmpRoleSnapshot)
    }

    private class TestStrings : AbstractStringEnumerator {
        private val ids = HashMap<String, Int>()
        private val values = mutableListOf<String>()
        override fun enumerate(value: String?): Int {
            if (value == null) return 0
            return ids.getOrPut(value) { values += value; values.size }
        }
        override fun valueOf(idx: Int): String? = if (idx == 0) null else values[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }
}

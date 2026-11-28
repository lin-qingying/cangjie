package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirErrorPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CjExprKind
import org.cangnova.cangjie.parsing.CjMacroFragmentParent
import org.cangnova.cangjie.parsing.MacroParsingFacts
import org.cangnova.cangjie.parsing.MacroParsingScope
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 准备阶段不可见；提交和恢复只影响实际 owner/槽位，不撤销其他已提交产品。 */
class CfirPreparedPublicationTest {
    /** 错误主构造器保留函数语义父类，诊断 holder 身份不能遮蔽正文和参数槽位。 */
    @Test
    fun errorPrimaryConstructorDelegatesToFunctionAndRegistersItsActualSlots() {
        val fixture = Fixture()
        val diagnostic = ConeSimpleDiagnostic("Missing primary constructor")
        val constructor = fixture.errorPrimaryConstructor(diagnostic)
        val bodyCall = buildFunctionCall {
            source = TestSource("constructor-body")
            calleeReference = buildNamedReference { name = Name.identifier("work") }
            argumentList = buildArgumentList()
        }
        val defaultValue = buildFunctionCall {
            source = TestSource("constructor-default")
            calleeReference = buildNamedReference { name = Name.identifier("seed") }
            argumentList = buildArgumentList()
        }
        val body = fixture.body(bodyCall)
        constructor.replaceBody(body)
        val parameter = fixture.parameter(constructor, defaultValue)
        val receivedFunctions = mutableListOf<CfirFunction>()
        constructor.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) = element.acceptChildren(this)

            override fun visitFunction(function: CfirFunction) {
                receivedFunctions += function
            }
        })

        val file = fixture.file(constructor)
        val psiIdentity = Any()
        fixture.store.recordExpressionSurfaces(file, psiIdentity, emptyList())
        assertAll(
            {
                assertEquals(1, receivedFunctions.size, "An error constructor must dispatch through its function parent")
                assertSame(constructor, receivedFunctions.single())
            },
            { assertSame(diagnostic, constructor.diagnostic) },
            { assertSame(body, constructor.body) },
            { assertSame(defaultValue, constructor.valueParameters.single().defaultValue) },
            {
                fixture.store.validateAdoptionSlots(
                    file,
                    listOf(
                        CfirRawMacroUsageSlot(constructor, CfirRawMacroUsageSlot.Kind.BODY),
                        CfirRawMacroUsageSlot(parameter, CfirRawMacroUsageSlot.Kind.PARAMETER_DEFAULT),
                    ),
                )
            },
        )
    }

    /** 真实子类型必须登记父类 owner 槽位；拒绝文件外 owner，不能放宽注册验证。 */
    @Test
    fun rawFileRegistrationRecognizesFunctionAndVariableSubtypes() {
        val fixture = Fixture()
        val function = fixture.function("function")
        val oldBodyMacro = fixture.macro(function)
        val oldInitializer = fixture.macro(function, inHeader = true)
        val originalBody = fixture.body(oldBodyMacro.call)
        function.replaceBody(originalBody)
        val variable = fixture.variable(oldInitializer.call)
        val file = fixture.file(function, variable)
        val psiIdentity = Any()
        val originalRecords = listOf(oldBodyMacro.surface, oldInitializer.surface)
        fixture.store.recordExpressionSurfaces(file, psiIdentity, originalRecords)
        val slots = listOf(
            fixture.bodySlot(function),
            CfirRawMacroUsageSlot(variable, CfirRawMacroUsageSlot.Kind.INITIALIZER),
        )
        fixture.store.validateAdoptionSlots(file, slots)
        val unregistered = fixture.function("unregistered")
        assertThrows(IllegalArgumentException::class.java) {
            fixture.store.validateAdoptionSlots(file, listOf(fixture.bodySlot(unregistered)))
        }

        val newBodyMacro = fixture.macro(function)
        val newInitializer = fixture.macro(function, inHeader = true)
        function.replaceBody(fixture.body(newBodyMacro.call))
        variable.replaceInitializer(newInitializer.call)
        val publication = fixture.store.prepareExpressionSurfacePublication(
            file, slots, listOf(newBodyMacro.surface, newInitializer.surface),
        )
        assertSurfaces(originalRecords, fixture.store.expressionSurfaces(file))
        publication.commit()
        val committedRecords = listOf(newBodyMacro.surface, newInitializer.surface)
        assertSurfaces(committedRecords, fixture.store.expressionSurfaces(file))
        assertSurfaces(committedRecords, fixture.store.expressionSurfacesForSourceFile(psiIdentity))

        function.replaceBody(originalBody)
        variable.replaceInitializer(oldInitializer.call)
        publication.restore()
        assertSurfaces(originalRecords, fixture.store.expressionSurfaces(file))
        assertSurfaces(originalRecords, fixture.store.expressionSurfacesForSourceFile(psiIdentity))
    }

    @Test
    fun metadataPreparationIsInvisibleAndRestoreAllowsRepublishingTheSameProduct() {
        val fixture = Fixture()
        val owner = fixture.function("owner")
        val annotation = fixture.annotate(owner)
        val product = fixture.metadataProduct(owner)
        val publication = fixture.registry.preparePublication(listOf(product))
        publication.validate()
        assertNull(fixture.registry.snapshot(annotation))

        publication.commit()
        val committed = requireNotNull(fixture.registry.snapshot(annotation))
        assertAll(
            { assertSame(owner, committed.owner) },
            { assertSame(annotation, committed.originalAnnotation) },
            { assertSame(annotation.source, committed.annotationSource) },
            { assertSame(annotation, owner.annotations[committed.annotationIndex]) },
        )
        publication.restore()
        assertNull(fixture.registry.snapshot(annotation))
        assertSame(annotation, owner.annotations.single())

        // 不读取私有 owner 表：同一封闭产品可重新提交，证明 owner 表和发布身份都已恢复。
        val republishing = fixture.registry.preparePublication(listOf(product))
        assertNull(fixture.registry.snapshot(annotation))
        republishing.commit()
        assertSame(committed, fixture.registry.snapshot(annotation))
    }

    @Test
    fun aChangedSecondAnnotationSlotRejectsTheWholePreparedProduct() {
        val fixture = Fixture()
        val first = fixture.function("first")
        val second = fixture.function("second")
        val firstAnnotation = fixture.annotate(first)
        val secondAnnotation = fixture.annotate(second)
        val product = fixture.metadataProduct(first, second)
        val publication = fixture.registry.preparePublication(listOf(product))
        publication.validate()
        val changedAnnotation = fixture.newAnnotation(second)
        second.replaceAnnotations(listOf(changedAnnotation))

        assertThrows(IllegalStateException::class.java) { publication.validate() }
        assertThrows(IllegalStateException::class.java) { publication.commit() }
        assertAll(
            { assertNull(fixture.registry.snapshot(firstAnnotation)) },
            { assertNull(fixture.registry.snapshot(secondAnnotation)) },
            { assertNull(fixture.registry.snapshot(changedAnnotation)) },
            { assertSame(firstAnnotation, first.annotations.single()) },
            { assertSame(changedAnnotation, second.annotations.single()) },
        )
        publication.restore()

        // 恢复调用方修改的槽位后重用原产品，排除首个 owner 留下不可见的半发布表。
        second.replaceAnnotations(listOf(secondAnnotation))
        fixture.registry.preparePublication(listOf(product)).commit()
        assertSame(first, fixture.registry.snapshot(firstAnnotation)?.owner)
        assertSame(second, fixture.registry.snapshot(secondAnnotation)?.owner)
    }

    @Test
    fun usagePublicationKeepsReusedPrefixAndUndoPreservesPeerHeaderAndAliasUpdates() {
        val fixture = Fixture()
        val target = fixture.function("target")
        val peer = fixture.function("peer")
        val oldPrefix = fixture.macro(target)
        val oldTail = fixture.macro(target)
        val oldPeer = fixture.macro(peer)
        val header = fixture.macro(target, inHeader = true)
        fixture.annotate(target, header.call)
        val originalBody = fixture.body(oldPrefix.call, oldTail.call)
        target.replaceBody(originalBody)
        peer.replaceBody(fixture.body(oldPeer.call))
        val file = fixture.file(target, peer)
        // 纯 CFIR 测试以不透明对象表示 PSI 身份；同一 PSI 可指向新的 raw 文件代次。
        val psiIdentity = Any()
        val originalRecords = listOf(oldPrefix.surface, oldTail.surface, oldPeer.surface, header.surface)
        fixture.store.recordExpressionSurfaces(file, psiIdentity, originalRecords)

        val discardedPrefix = fixture.macro(target)
        val newTail = fixture.macro(target)
        val adoptedBody = fixture.body(oldPrefix.call, newTail.call)
        target.replaceBody(adoptedBody)
        val publication = fixture.store.prepareExpressionSurfacePublication(
            file,
            listOf(fixture.bodySlot(target)),
            listOf(discardedPrefix.surface, newTail.surface),
        )
        publication.validate()
        assertSurfaces(originalRecords, fixture.store.expressionSurfaces(file))
        assertSurfaces(originalRecords, fixture.store.expressionSurfacesForSourceFile(psiIdentity))

        publication.commit()
        val committedRecords = listOf(oldPrefix.surface, newTail.surface, oldPeer.surface, header.surface)
        assertAll(
            { assertSame(adoptedBody, target.body) },
            { assertSurfaces(committedRecords, fixture.store.expressionSurfaces(file)) },
            { assertSurfaces(committedRecords, fixture.store.expressionSurfacesForSourceFile(psiIdentity)) },
            { assertFalse(fixture.store.expressionSurfaces(file).any { it.replaceHandle.carrier === discardedPrefix.call }) },
        )

        // 目标提交之后，另一槽位独立提交；目标恢复不能撤销该新 peer 产品。
        val newPeer = fixture.macro(peer)
        val peerBody = fixture.body(newPeer.call)
        peer.replaceBody(peerBody)
        fixture.store.prepareExpressionSurfacePublication(
            file,
            listOf(fixture.bodySlot(peer)),
            listOf(newPeer.surface),
        ).commit()
        assertSurfaces(
            listOf(oldPrefix.surface, newTail.surface, newPeer.surface, header.surface),
            fixture.store.expressionSurfacesForSourceFile(psiIdentity),
        )

        // 新 raw 文件代次更新同一 PSI 别名，并带有自己的 header；旧句柄只拥有旧文件槽位。
        val aliasOwner = fixture.function("replacementGeneration")
        val aliasBodyMacro = fixture.macro(aliasOwner)
        val aliasHeader = fixture.macro(aliasOwner, inHeader = true)
        fixture.annotate(aliasOwner, aliasHeader.call)
        aliasOwner.replaceBody(fixture.body(aliasBodyMacro.call))
        val aliasFile = fixture.file(aliasOwner)
        val aliasRecords = listOf(aliasBodyMacro.surface, aliasHeader.surface)
        fixture.store.recordExpressionSurfaces(aliasFile, psiIdentity, aliasRecords)

        // 图恢复由调用方负责，provider 恢复该句柄实际写入的 side table。
        target.replaceBody(originalBody)
        publication.restore()
        assertAll(
            { assertSame(originalBody, target.body) },
            { assertSame(peerBody, peer.body) },
            {
                assertSurfaces(
                    listOf(oldPrefix.surface, oldTail.surface, newPeer.surface, header.surface),
                    fixture.store.expressionSurfaces(file),
                )
            },
            { assertSurfaces(aliasRecords, fixture.store.expressionSurfaces(aliasFile)) },
            { assertSurfaces(aliasRecords, fixture.store.expressionSurfacesForSourceFile(psiIdentity)) },
        )
    }

    @Test
    fun aChangedSlotRootInvalidatesAValidatedPlanBeforeAnyUsageWrite() {
        val fixture = Fixture()
        val first = fixture.function("first")
        val second = fixture.function("second")
        val oldFirst = fixture.macro(first)
        val oldSecond = fixture.macro(second)
        first.replaceBody(fixture.body(oldFirst.call))
        second.replaceBody(fixture.body(oldSecond.call))
        val file = fixture.file(first, second)
        val psiIdentity = Any()
        val originalRecords = listOf(oldFirst.surface, oldSecond.surface)
        fixture.store.recordExpressionSurfaces(file, psiIdentity, originalRecords)
        val newFirst = fixture.macro(first)
        val newSecond = fixture.macro(second)
        val firstCandidate = fixture.body(newFirst.call)
        first.replaceBody(firstCandidate)
        second.replaceBody(fixture.body(newSecond.call))
        val publication = fixture.store.prepareExpressionSurfacePublication(
            file,
            listOf(fixture.bodySlot(first), fixture.bodySlot(second)),
            listOf(newFirst.surface, newSecond.surface),
        )
        publication.validate()

        // carrier 保持相同，根对象已被替换；旧计划不能对新的实际根提交。
        val changedRoot = fixture.body(newSecond.call)
        second.replaceBody(changedRoot)
        assertThrows(IllegalStateException::class.java) { publication.validate() }
        assertThrows(IllegalStateException::class.java) { publication.commit() }
        publication.restore()
        assertAll(
            { assertSame(firstCandidate, first.body) },
            { assertSame(changedRoot, second.body) },
            { assertSurfaces(originalRecords, fixture.store.expressionSurfaces(file)) },
            { assertSurfaces(originalRecords, fixture.store.expressionSurfacesForSourceFile(psiIdentity)) },
        )
    }

    private fun assertSurfaces(expected: List<MacroSurfaceExpr>, actual: List<MacroSurfaceExpr>) {
        assertEquals(expected.size, actual.size)
        expected.forEach { record ->
            assertTrue(actual.any { it === record }, "The actual committed usage record identity must be retained")
        }
    }

    /** 沿用 metadata transaction fixture 的真实 source module、声明、注解和封闭产品。 */
    private class Fixture {
        val session: CfirSession = object : CfirSession(Kind.Source) {}
        val registry = session.ensureAnnotationMetadataRegistry()
        val store = CfirRawMacroSurfaceUsageStore(session)
        private val packageName = FqName("prepared")
        private val moduleName = "prepared-publication"
        private val moduleData: CfirModuleData = CfirSourceModuleData(
            Name.identifier(moduleName), emptyList(), emptyList(), CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also { it.bindSession(session) }

        fun function(name: String): CfirNamedFunction = buildNamedFunction {
            source = TestSource("function:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(packageName, Name.identifier(name)))
            this.name = Name.identifier(name)
            isMut = false
        }

        fun errorPrimaryConstructor(diagnostic: ConeDiagnostic): CfirErrorPrimaryConstructor = buildErrorPrimaryConstructor {
            source = TestSource("error-primary-constructor")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirConstructorSymbol(CallableId(packageName, Name.special("<init>")))
            isPrimary = true
            this.diagnostic = diagnostic
        }

        fun parameter(owner: CfirFunction, defaultValue: CfirExpression): CfirValueParameter = buildValueParameter {
            source = TestSource("parameter:value")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            isNamed = true
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirValueParameterSymbol(CallableId(packageName, Name.identifier("value")))
            containingDeclarationSymbol = owner.symbol
            name = Name.identifier("value")
            this.defaultValue = defaultValue
        }.also { owner.replaceValueParameters(owner.valueParameters + it) }

        fun file(vararg declarations: CfirDeclaration): CfirFile = buildFile {
            source = TestSource("file:publication.cj")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "publication.cj"
            packageDirective = buildPackageDirective {
                packageFqName = packageName
                isMacroPackage = false
            }
            this.declarations += declarations
        }

        /** 顶层 let _ 的实际模式变量根拥有 initializer 槽位，不制造局部或成员 owner。 */
        fun variable(initializer: CfirExpression): CfirPatternVariable = buildPatternVariable {
            source = TestSource("variable")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirPatternVariableSymbol(CallableId(packageName, Name.special("<pattern-variable>")))
            pattern = buildWildcardPattern { source = TestSource("wildcard") }
            isVar = false
            this.initializer = initializer
        }

        fun newAnnotation(owner: CfirNamedFunction): CfirAnnotationCall = buildAnnotationCall {
            source = TestSource("annotation")
            typeRef = buildImplicitTypeRef()
            annotationSourceName = "Tag"
            sourceModuleName = moduleName
            forcedCustom = true
            isCompileTimeVisible = true
            argumentList = buildArgumentList()
            calleeReference = buildNamedReference { name = Name.identifier("Tag") }
            containingDeclarationSymbol = owner.symbol
        }

        fun annotate(owner: CfirNamedFunction, vararg arguments: CfirExpression): CfirAnnotationCall =
            newAnnotation(owner).also { annotation ->
                annotation.replaceArgumentList(buildArgumentList { this.arguments += arguments })
                owner.replaceAnnotations(owner.annotations + annotation)
            }

        fun metadataProduct(vararg owners: CfirNamedFunction): CfirAnnotationMetadataSnapshot {
            val builder = CfirAnnotationMetadataBuilder()
            owners.forEach { owner ->
                owner.annotations.forEachIndexed { index, annotation ->
                    val call = annotation as CfirAnnotationCall
                    builder.record(CfirAnnotationSlotSnapshot(
                        owner = owner,
                        annotationIndex = index,
                        originalAnnotation = call,
                        rawSyntax = "@!Tag",
                        forcedCustom = true,
                        isCompileTimeVisible = true,
                        annotationSource = requireNotNull(call.source),
                        qualifiedName = FqName("prepared.Tag"),
                        argumentText = null,
                        tokens = listOf(MacroSurfaceToken("@!Tag", 0, 5)),
                        callSite = MacroCallSite.DECLARATION,
                    ))
                }
            }
            return builder.seal(listOf(file(*owners)))
        }

        fun body(vararg expressions: CfirExpression): CfirBlock = buildBlock {
            source = TestSource("body")
            statements += expressions
        }

        fun bodySlot(owner: CfirNamedFunction): CfirRawMacroUsageSlot =
            CfirRawMacroUsageSlot(owner, CfirRawMacroUsageSlot.Kind.BODY)

        /** surface 句柄绑定实际表达式；正文快照持有真实函数目标，header 使用顶层词法语境。 */
        fun macro(owner: CfirNamedFunction, inHeader: Boolean = false): Macro {
            val call = buildFunctionCall {
                source = TestSource("macro")
                calleeReference = buildNamedReference { name = Name.identifier("M") }
                argumentList = buildArgumentList()
            }
            val id = MacroSurfaceIdGenerator.next()
            val snapshot = if (inHeader) topLevelMacroOwnerSnapshot(packageName) else MacroRawOwnerSnapshot(
                packageFqName = packageName,
                containerSymbols = listOf(owner.symbol),
                dispatchReceiverType = null,
                inLocalContext = false,
                functionTarget = CfirFunctionTarget(isLambda = false).also { it.bind(owner) },
                signature = null,
                nominalSyntax = null,
                capturedTypeParameters = emptyList(),
                constructedClasses = emptyList(),
            )
            return Macro(call, MacroSurfaceExpr(
                surfaceId = id,
                qualifiedName = FqName("prepared.M"),
                isQualifiedName = false,
                kind = MacroSurface.Kind.PLAIN,
                parsingFacts = MacroParsingFacts(
                    MacroParsingScope.Expression(CjExprKind.UNKNOWN_EXPR), CjMacroFragmentParent.OTHER, null,
                ),
                hasParenthesis = true,
                attrTokens = emptyList(),
                inputTokens = emptyList(),
                sourceRange = MacroSurfaceSourceRange(call.source, 0, 4),
                scopeContext = MacroSurfaceScopeContext(packageName, moduleName, null, if (inHeader) null else owner.name),
                modifiers = emptyList(),
                carriedAnnotations = emptyList(),
                capturedRawSyntax = "@M()",
                containerContext = MacroSurfaceContainerContext(
                    outerDeclarationKind = if (inHeader) MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL
                        else MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
                    isInsidePrimaryConstructor = false,
                    isInsideEnumBody = false,
                    isInsideBlock = !inHeader,
                ),
                replaceHandle = CfirReplaceHandle(id, snapshot, call),
            ))
        }
    }

    private class Macro(val call: CfirFunctionCall, val surface: MacroSurfaceExpr)

    private class TestSource(identity: String) : CjBinarySourceElement(identity, null, identity)
}

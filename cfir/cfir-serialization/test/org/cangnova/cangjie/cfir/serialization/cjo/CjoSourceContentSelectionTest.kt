package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 原生 ASTWriter 的阶段、词法 frozen 和默认 assignment 来源契约；不根据 codec 能力决定资格。 */
@OptIn(CfirImplementationDetail::class)
class CjoSourceContentSelectionTest {
    @Test
    fun reservingADeclarationDoesNotSelectItsContent() {
        val fixture = Fixture()
        val function = fixture.function("f", frozen = true)
        val selection = fixture.selection(function)
        assertTrue(selection.isPreContentFunctionCandidate(function))
        assertThrows(IllegalStateException::class.java) { selection.sourceContent(function) }
        assertThrows(NoSuchElementException::class.java) { selection.sourceSaveStageOf(function) }
    }

    @Test
    fun firstSaveAtFinalCannotBePromotedToPreContent() {
        val fixture = Fixture()
        val function = fixture.function("f", frozen = true)
        val selection = fixture.selection(function)
        assertNull(selection.selectSourceContent(function, CjoSourceSaveStage.FINAL))
        assertNull(selection.selectSourceContent(function, CjoSourceSaveStage.PRE_CONTENT))
        assertEquals(CjoSourceSaveStage.FINAL, selection.sourceSaveStageOf(function))
    }

    @Test
    fun finalSaveReusesThePreContentRootAndStage() {
        val fixture = Fixture()
        val function = fixture.function("f", frozen = true)
        val selection = fixture.selection(function)
        val original = requireNotNull(selection.selectSourceContent(function, CjoSourceSaveStage.PRE_CONTENT))
        function.replaceBody(fixture.block(fixture.literal(99L)))
        assertSame(original, selection.selectSourceContent(function, CjoSourceSaveStage.FINAL))
        assertSame(original, selection.sourceContent(function))
        assertEquals(CjoSourceSaveStage.PRE_CONTENT, original.stage)
        assertEquals(CjoSourceContentSlot.FUNCTION_BODY, original.slot)
    }

    @Test
    fun lambdaBodyUsesTheActualSaveStage() {
        val fixture = Fixture()
        val lambda = fixture.lambda()
        val lambdaType = ConeFunctionType(emptyList(), ConePrimitiveType.INT64)
        val owner = fixture.function("f", frozen = true, returnType = lambdaType)
        owner.replaceBody(fixture.block(buildAnonymousFunctionExpression {
            source = Source("lambda-expression")
            isTrailingLambda = false
            anonymousFunction = lambda
        }, type = lambdaType))
        val analysis = fixture.analysis(owner)
        val before = fixture.selection(analysis)
        assertFalse(before.isPreContentFunctionCandidate(lambda))
        assertSame(fixture.identity(checkNotNull(lambda.body)), before.selectSourceContent(lambda, CjoSourceSaveStage.PRE_CONTENT)?.root)
        val final = fixture.selection(analysis)
        assertNull(final.selectSourceContent(lambda, CjoSourceSaveStage.FINAL))
    }

    @Test
    fun lexicalFrozenAssignmentOverridesTheNestedFunctionsOwnFlag() {
        for ((outerFrozen, nestedFrozen) in listOf(false to true, true to false)) {
            val fixture = Fixture()
            val nested = fixture.function("nested", frozen = nestedFrozen, local = true)
            val outer = fixture.function("outer", frozen = outerFrozen)
            outer.replaceBody(fixture.block(nested, fixture.literal(7L)))
            val analysis = fixture.analysis(outer)
            assertEquals(nestedFrozen, analysis.isFrozenOf(nested))
            val content = fixture.selection(analysis).selectSourceContent(nested, CjoSourceSaveStage.PRE_CONTENT)
            assertEquals(outerFrozen, content != null)
            assertEquals(nestedFrozen, analysis.isFrozenOf(nested), "writer 不能回写原始 frozen")
        }
    }

    @Test
    fun constOwnerAndCalleeDoNotSetTheOriginalAssignmentsConstFlag() {
        for (toolAdded in listOf(false, true)) {
            val fixture = Fixture()
            val callee = fixture.function("constant", const = true)
            val function = fixture.function("f", const = true)
            val assignment = buildFunctionCall {
                source = Source("default-call")
                coneTypeOrNull = ConePrimitiveType.INT64
                calleeReference = buildResolvedNamedReference {
                    source = Source("constant-reference")
                    name = callee.name
                    resolvedSymbol = callee.symbol
                }
            }
            val parameter = fixture.parameter(function, assignment)
            parameter.defaultAssignmentSyntaxFacts = CfirDefaultAssignmentSyntaxFacts(isLiteral = false, literalConst = false)
            parameter.exportFacts = parameter.exportFacts.copy(isToolAdded = toolAdded)
            function.replaceValueParameters(listOf(parameter))
            val analysis = fixture.analysis(callee, function)
            val facts = analysis.defaultAssignmentFactsOf(parameter)
            assertFalse(facts.assignmentIsConst)
            assertFalse(facts.assignmentIsLiteral)
            assertEquals(toolAdded, facts.parameterIsToolAdded)
            val content = fixture.selection(analysis).selectSourceContent(parameter, CjoSourceSaveStage.FINAL)
            assertEquals(toolAdded, content != null)
            if (toolAdded) {
                assertSame(fixture.identity(assignment), content?.root)
                assertEquals(CjoSourceContentSlot.PARAMETER_DEFAULT, content?.slot)
            }
        }
    }

    @Test
    fun unrecordedOriginalAssignmentCannotBeInferredFromALiteralAfterNormalization() {
        val fixture = Fixture()
        val function = fixture.function("f", const = true)
        val parameter = fixture.parameter(function, fixture.literal(7L))
        function.replaceValueParameters(listOf(parameter))
        val selection = fixture.selection(function)
        assertThrows(IllegalStateException::class.java) { selection.selectSourceContent(parameter, CjoSourceSaveStage.FINAL) }
    }

    @Test
    fun parameterDefaultsDoNotUseTheFunctionContentSwitch() {
        val fixture = Fixture()
        val function = fixture.function("f")
        val value = fixture.literal(7L)
        val parameter = fixture.parameter(function, value)
        parameter.defaultAssignmentSyntaxFacts = CfirDefaultAssignmentSyntaxFacts(isLiteral = true, literalConst = true)
        function.replaceValueParameters(listOf(parameter))
        val selection = fixture.selection(fixture.analysis(function), exportContent = false)
        assertSame(fixture.identity(value), selection.selectSourceContent(parameter, CjoSourceSaveStage.FINAL)?.root)
        assertNull(selection.selectSourceContent(function, CjoSourceSaveStage.FINAL))
    }

    @Test
    fun toolAddedDefaultDoesNotRequireUnknownConstFacts() {
        val fixture = Fixture()
        val function = fixture.function("f")
        val value = fixture.literal(7L)
        val parameter = fixture.parameter(function, value)
        parameter.exportFacts = parameter.exportFacts.copy(isToolAdded = true)
        assertNull(parameter.defaultAssignmentSyntaxFacts)
        function.replaceValueParameters(listOf(parameter))
        val selection = fixture.selection(fixture.analysis(function), exportContent = false)
        assertSame(fixture.identity(value), selection.selectSourceContent(parameter, CjoSourceSaveStage.FINAL)?.root)
    }

    /** SetOuterFunctionDecl 从真实外围函数进入局部 initializer，不把局部变量或 lambda 当作 native fd。 */
    @Test
    fun nestedFunctionInLocalLambdaInitializerUsesTheOuterFunction() {
        val fixture = Fixture()
        val nested = fixture.function("nested", local = true)
        val lambda = fixture.lambda()
        lambda.replaceBody(fixture.block(nested, fixture.literal(7L)))
        val lambdaType = ConeFunctionType(emptyList(), ConePrimitiveType.INT64)
        val holder = fixture.variable("holder", lambdaType, buildAnonymousFunctionExpression {
            source = Source("lambda-expression")
            isTrailingLambda = false
            anonymousFunction = lambda
        }, local = true)
        val outer = fixture.function("outer", frozen = true)
        outer.replaceBody(fixture.block(holder, fixture.literal(7L)))
        val analysis = fixture.analysis(outer)
        assertSame(lambda, analysis.ownerOf(nested))
        assertSame(holder, analysis.ownerOf(lambda))
        assertNull(analysis.nativeOuterOf(holder))
        assertSame(outer, analysis.nativeOuterOf(nested))
        assertTrue(analysis.shouldExportSource(holder))
        assertSame(fixture.identity(checkNotNull(nested.body)), fixture.selection(analysis).selectSourceContent(nested, CjoSourceSaveStage.PRE_CONTENT)?.root)
    }

    /** 包级简单模式只是语法壳，原生函数 outer 指向真正的 VarDecl binding。 */
    @Test
    fun nestedFunctionInGlobalPatternInitializerUsesTheBinding() {
        val fixture = Fixture()
        val nested = fixture.function("nested", local = true)
        val lambda = fixture.lambda()
        lambda.replaceBody(fixture.block(nested, fixture.literal(7L)))
        val lambdaType = ConeFunctionType(emptyList(), ConePrimitiveType.INT64)
        val (holder, binding) = fixture.patternVariable("holder", lambdaType, buildAnonymousFunctionExpression {
            source = Source("lambda-expression")
            isTrailingLambda = false
            anonymousFunction = lambda
        })
        val analysis = fixture.analysis(holder)
        assertSame(lambda, analysis.ownerOf(nested))
        assertSame(holder, analysis.ownerOf(lambda))
        assertSame(binding, analysis.nativeOuterOf(nested))
    }

    @Test
    fun macroPackageCannotEnterPreContent() {
        val fixture = Fixture()
        val function = fixture.function("f", frozen = true)
        val selection = fixture.selection(fixture.analysis(function), macroPackage = true)
        assertFalse(selection.isPreContentFunctionCandidate(function))
        assertThrows(IllegalArgumentException::class.java) { selection.selectSourceContent(function, CjoSourceSaveStage.PRE_CONTENT) }
        assertNull(selection.selectSourceContent(function, CjoSourceSaveStage.FINAL))
    }

    private class Fixture {
        private val session = object : CfirSession(Kind.Source) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
        }
        private lateinit var canonical: CfirSourceGraph
        fun identity(expression: CfirElement): CfirSourceExpressionIdentity = canonical.expressionOf(expression)
        private val module = CfirSourceModuleData(
            Name.identifier("source-selection"), emptyList(), emptyList(),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also { it.bindSession(session) }
        private val packageName = FqName("selection")

        fun function(name: String, frozen: Boolean = false, const: Boolean = false, local: Boolean = false,
                     returnType: ConeCangJieType = ConePrimitiveType.INT64): CfirNamedFunction = buildNamedFunction {
            source = Source("function:$name")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = local
            status = status(const)
            returnTypeRef = typeRef(returnType)
            symbol = CfirNamedFunctionSymbol(if (local) CallableId(Name.identifier(name)) else CallableId(packageName, Name.identifier(name)))
            this.name = Name.identifier(name)
            isMut = false
            body = block(literal(7L), type = returnType)
        }.also { it.interopInfo = CfirInteropInfo.NONE.copy(isFrozen = frozen) }

        fun lambda(): CfirAnonymousFunction = buildAnonymousFunction {
            source = Source("lambda")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = true
            status = status()
            returnTypeRef = typeRef(ConePrimitiveType.INT64)
            symbol = CfirAnonymousFunctionSymbol()
            hasExplicitParameterList = true
            isLambda = true
            typeRef = typeRef(ConeFunctionType(emptyList(), ConePrimitiveType.INT64))
            body = block(literal(7L))
        }

        fun parameter(owner: CfirFunction, value: CfirExpression): CfirValueParameter = buildValueParameter {
            source = Source("parameter")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            isNamed = true
            name = Name.identifier("p")
            symbol = CfirValueParameterSymbol(CallableId(name))
            containingDeclarationSymbol = owner.symbol
            returnTypeRef = typeRef(ConePrimitiveType.INT64)
            defaultValue = value
        }

        fun variable(name: String, type: ConeCangJieType, initializer: CfirExpression, local: Boolean): CfirFieldVariable = buildFieldVariable {
            source = Source("variable:$name")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = local
            status = status()
            this.name = Name.identifier(name)
            symbol = CfirFieldVariableSymbol(if (local) CallableId(this.name) else CallableId(packageName, this.name))
            returnTypeRef = typeRef(type)
            isVar = false
            this.initializer = initializer
        }

        fun patternVariable(name: String, type: ConeCangJieType, initializer: CfirExpression): Pair<CfirPatternVariable, CfirPatternBindingVariable> {
            val binding = buildPatternBindingVariable {
                source = Source("binding:$name")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = status()
                this.name = Name.identifier(name)
                symbol = CfirPatternBindingSymbol(CallableId(packageName, this.name))
                returnTypeRef = typeRef(type)
                isVar = false
            }
            val holder = buildPatternVariable {
                source = Source("pattern:$name")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = status()
                symbol = CfirPatternVariableSymbol(CallableId(packageName, Name.special("<pattern-variable>")))
                returnTypeRef = typeRef(type)
                isVar = false
                pattern = buildBindingPattern { this.name = binding.name; bindingVariable = binding }
                this.initializer = initializer
            }
            return holder to binding
        }

        fun literal(value: Long): CfirLiteralExpression = buildLiteralExpression {
            source = Source("literal:$value")
            coneTypeOrNull = ConePrimitiveType.INT64
            kind = CfirLiteralKind.INT
            this.value = value
        }

        fun block(vararg statements: CfirStatement, type: ConeCangJieType = ConePrimitiveType.INT64): CfirBlock = buildBlock {
            source = Source("block")
            coneTypeOrNull = type
            this.statements += statements
        }

        /** 正式 export 入口直接捕获已完成文件的归属和默认值事实；真实 Provider 为类型展开提供 session 组件。 */
        fun analysis(vararg declarations: CfirDeclaration): CfirDeclarationExportAnalysis {
            val file = buildFile {
                source = Source("file")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                symbol = CfirFileSymbol()
                name = "selection.cj"
                packageDirective = buildPackageDirective { packageFqName = packageName; isMacroPackage = false }
                this.declarations += declarations
            }
            val provider = CfirProviderImpl(session, CfirCangJieScopeProvider())
            session.register(CfirProvider::class, provider)
            session.register(CfirSymbolProvider::class, provider.symbolProvider)
            canonical = CfirSourceGraphProducer.produce(listOf(file))
            return CfirExportAnalysis.analyze(listOf(file), settings = CfirExportAnalysisSettings(canInline = false),
                canonicalSources = listOf(canonical))
        }

        fun selection(vararg declarations: CfirDeclaration): CjoDeclarationExportSelection = selection(analysis(*declarations))

        fun selection(analysis: CfirDeclarationExportAnalysis, macroPackage: Boolean = false, exportContent: Boolean = true): CjoDeclarationExportSelection =
            CjoDeclarationExportSelection.create(
                analysis, CjoExportOptions(isMacroPackage = macroPackage, exportContent = exportContent),
                isSuppressed = { false }, isMatchedCommon = { false },
            )

        private fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }

        private fun status(const: Boolean = false): CfirResolvedDeclarationStatus = buildResolvedDeclarationStatus {
            visibility = Visibilities.Public
            visibilityExplicitness = CfirModifierExplicitness.EXPLICIT
            modalityExplicitness = CfirModifierExplicitness.IMPLICIT
            bodyPresence = CfirBodyPresence.UNKNOWN
            isDeclarationFile = false
            isFromCommonPart = false
            isVisibilityExplicit = true
            isModalityExplicit = false
            isAbstractExplicit = false
            isOverride = false
            isOperator = false
            isStatic = false
            isConst = const
            isMut = false
            isUnsafe = false
            isForeign = false
            isC = false
            isCommon = false
            isSpecific = false
            isCommonWithDefault = false
            isRedef = false
            isDefault = false
            isAbstract = false
            isOpen = false
            isSealed = false
            modality = Modality.FINAL
        }
    }

    private class Source(identity: String) : CjBinarySourceElement(identity, null, identity)
}

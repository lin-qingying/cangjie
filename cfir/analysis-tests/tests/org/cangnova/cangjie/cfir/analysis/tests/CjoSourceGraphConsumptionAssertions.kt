package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.*
import PackageFormat.Package
import com.google.flatbuffers.Table
import com.intellij.openapi.Disposable
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportSourceSnapshots
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.source.CjSourceLocation
import org.cangnova.cangjie.name.OperatorNameConventions
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.IdentityHashMap

/**
 * fresh CJO 的正式 SOURCE 消费验收。临时 UTF-8 consumer 通过公开 frontend 装配正常 session。
 * 下方只比较已完成 typed 图与原 handler 已验收的 wire 事实，不生成或解析 SOURCE 图。
 * 固定语义依据：原 source-expression-export/FIXTURE.md 及
 * docs/cjmp-source-helper-20261117/s5-astloader-source-native-authority-20261119.md
 * 的 native 14276740 SOURCE33 合同；原 handler 的 table/edge/count/SHA 期望保持独立。
 */
internal object CjoSourceGraphConsumptionAssertions {
    @OptIn(CompilerConfiguration.Internals::class)
    fun assertConsumed(
        producerConfiguration: CompilerConfiguration,
        disposable: Disposable,
        libraryRoot: Path,
        cjo: Path,
        sourceOwner: CfirInterface,
        methodNames: List<String>,
    ) {
        val packageName = sourceOwner.symbol.classId.packageFqName.asString()
        val consumerPackage = "${packageName}_consumer"
        val consumerSource = libraryRoot.resolve("sourceGraphConsumer.cj")
        val ownerName = sourceOwner.name.asString()
        Files.writeString(consumerSource,
            "package $consumerPackage\n\nimport $packageName.*\n\nfunc consumeSourceGraphOwner(value: $ownerName): $ownerName { return value }\n",
            Charsets.UTF_8)
        val diagnostics = DiagnosticsCollectorImpl()
        val messages = ConsumptionMessages()
        val configuration = CompilerConfiguration().apply {
            moduleName = consumerPackage
            languageVersionSettings = producerConfiguration.languageVersionSettings
            useLightTree = producerConfiguration.useLightTree
            cjmpMode = producerConfiguration.cjmpMode
            diagnosticsCollector = diagnostics
            messageCollector = messages
            producerConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
            addClasspathRoot(libraryRoot.toAbsolutePath().normalize().toString())
            addCangJieSourceRoot(consumerSource.toAbsolutePath().normalize().toString())
        }
        assertNull(configuration.cjoOutputDirectory, "consumer 只读取 fresh CJO")
        val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
        assertTrue(diagnostics.diagnostics.isEmpty(), diagnostics.diagnostics.joinToString("\n") { "${it.factoryName}: ${it.renderMessage()}" })
        assertTrue(messages.entries.none { it.first.isWarning || it.first.isError }, messages.entries.toString())
        val output = checkNotNull(artifact) { "实际使用 imported interface 的 consumer 必须完成正式 frontend" }.frontendOutput.outputs.single()
        val consumer = output.session
        assertEquals(CfirSession.Kind.Source, consumer.kind)
        assertNotSame(sourceOwner.moduleData.session, consumer)
        val consumerFile = output.fir.single()
        assertEquals(consumerPackage, consumerFile.packageDirective.packageFqName.asString())
        val consumerFunction = assertInstanceOf(CfirNamedFunction::class.java, consumerFile.declarations.single())
        assertEquals("consumeSourceGraphOwner", consumerFunction.name.asString())
        assertEquals(CfirDeclarationOrigin.Source, consumerFunction.origin)
        assertSame(consumer, consumerFunction.moduleData.session)
        val owner = checkNotNull(consumer.symbolProvider.getClassLikeSymbolByClassId(sourceOwner.symbol.classId)?.cfir as? CfirInterface)
        val library = owner.moduleData.session
        assertEquals(CfirSession.Kind.Library, library.kind)
        assertNotSame(consumer, library)
        assertNotSame(sourceOwner.moduleData.session, library)
        assertEquals(CfirDeclarationOrigin.Library, owner.origin)
        assertNotSame(sourceOwner.symbol, owner.symbol, "消费声明必须来自 fresh CJO 的正式 library session")
        assertSame(library, owner.moduleData.session)
        val methods = owner.declarations.filterIsInstance<CfirNamedFunction>()
        assertEquals(methodNames, methods.map { it.name.asString() })
        assertTrue(methods.isNotEmpty(), "正式 SOURCE 消费必须实际请求选中函数")
        val snapshot = checkNotNull(CjoExportSourceSnapshots.loadedPackage(library.symbolProvider, cjo))
        assertEquals(cjo.toAbsolutePath().normalize(), snapshot.sourcePath.toAbsolutePath().normalize())
        val headerParameters = methods.map { it.valueParameters.toList() }
        val headerParameterTypes = headerParameters.map { parameters -> parameters.map { it.returnTypeRef.coneType } }
        val headerReturnTypes = methods.map { it.returnTypeRef.coneType }
        val provider = checkNotNull(methods.first().moduleData.session.importedSourceGraphProvider)
        val graph = checkNotNull(provider.packageSourceGraphOf(methods.first())) { "显式整包 SOURCE 产品不能返回 null" }
        for (method in methods) {
            assertTrue(method.status.isDefault)
            assertSame(library, method.moduleData.session)
            assertSame(provider, method.moduleData.session.importedSourceGraphProvider)
            assertSame(graph, provider.packageSourceGraphOf(method), "显式同包请求必须消费同一 context 已完成的图")
            assertSame(graph, provider.packageSourceGraphOf(method), "重复请求不能重建正文身份")
            assertTrue(graph.functionBodies.containsKey(method), "${method.name} 的 selectedBody 必须存在")
            val body = graph.functionBodies.getValue(method)
            assertTrue(graph.expression(body).payload is CfirSourceExpressionPayload.Block, "selected SOURCE 必须恢复 typed Block")
            assertTrue(graph.expression(body).type.isNothing)
            assertSame(body, graph.functionBodies.getValue(method))
            assertSame(graph.expression(body), graph.expression(body), "重复查询保留完成记录身份")
        }
        Consumption(graph, snapshot, library).assertComplete()
        methods.forEachIndexed { ordinal, method ->
            assertSame(headerReturnTypes[ordinal], method.returnTypeRef.coneType, "SOURCE 不能替换已加载的签名类型")
            assertEquals(headerParameters[ordinal].size, method.valueParameters.size)
            method.valueParameters.forEachIndexed { argument, parameter ->
                assertSame(headerParameters[ordinal][argument], parameter, "header/source 共用参数声明缓存")
                assertSame(headerParameterTypes[ordinal][argument], parameter.returnTypeRef.coneType, "header/source 共用参数类型缓存")
            }
        }
        assertSame(snapshot, CjoExportSourceSnapshots.loadedPackage(library.symbolProvider, cjo), "header/source 必须持有同代快照")
        assertSame(owner, consumer.symbolProvider.getClassLikeSymbolByClassId(sourceOwner.symbol.classId)?.cfir)
        assertTrue(diagnostics.diagnostics.isEmpty(), "按需 SOURCE 消费不得新增诊断")
        assertTrue(messages.entries.none { it.first.isWarning || it.first.isError }, messages.entries.toString())
    }

    private class ConsumptionMessages : MessageCollector {
        val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        override fun clear() = entries.clear()
        override fun hasErrors(): Boolean = entries.any { it.first.isError }
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            entries += severity to message
        }
    }

    /** 对应关系只用于断言：e/d/t 分别登记，绝不依据数字相同合并身份。 */
    private class Consumption(
        private val graph: CfirSourceGraph,
        private val snapshot: CjoLoadedPackage,
        private val library: CfirSession,
    ) {
        private val pkg: Package = snapshot.pkg
        private val expressions = mutableMapOf<UInt, CfirSourceExpressionIdentity>()
        private val expressionIndices = IdentityHashMap<CfirSourceExpressionIdentity, UInt>()
        private val declarations = mutableMapOf<UInt, CfirDeclaration>()
        private val types = mutableMapOf<UInt, ConeCangJieType>()
        private val lambdaOwners = java.util.Collections.newSetFromMap(IdentityHashMap<CfirFunction, Boolean>())

        fun assertComplete() {
            val selected = (1..pkg.allDeclsLength).map { it.toUInt() }.filter { index ->
                val declaration = decl(index)
                declaration.kind == DeclKind.FuncDecl && functionBody(declaration).body != 0u
            }.toSet()
            val importedBodies = graph.functionBodies.keys.filter { it.cjoDeclarationOrigin != null }
            assertEquals(selected, importedBodies.map { currentIndex(it) }.toSet(), "全部 selected FuncBody 必须恢复；不能只加载入口方法")
            assertEquals(selected.size, importedBodies.size)
            importedBodies.forEach { declaration(currentIndex(it), it) }
            graph.initializers.forEach { (variable, identity) ->
                val index = currentIndex(variable)
                declaration(index, variable)
                val wire = decl(index)
                val initializer = if (wire.kind == DeclKind.VarWithPatternDecl)
                    declInfo(wire, VarWithPatternInfo()).initializer else declInfo(wire, VarInfo()).initializer
                expression(initializer, identity, variable)
            }
            graph.defaults.forEach { (parameter, identity) ->
                val index = currentIndex(parameter)
                declaration(index, parameter)
                expression(declInfo(decl(index), ParamInfo()).defaultVal, identity, parameter)
            }
            assertEquals((1..pkg.allExprsLength).map { it.toUInt() }.toSet(), expressions.keys,
                "完整 fixture 的 initializer/lambda/pattern/辅助记录与回边不能在消费时丢失")
            assertEquals(graph.expressions.keys, expressionIndices.keys, "typed 图不能混入额外壳或未对应的节点")
            assertEquals(lambdaOwners, graph.functionBodies.keys.filter { it.cjoDeclarationOrigin == null }.toSet())
            graph.functionBodies.keys.filterIsInstance<CfirNamedFunction>().forEach { function ->
                assertSame(graph, checkNotNull(function.moduleData.session.importedSourceGraphProvider).packageSourceGraphOf(function))
            }
            declarations.values.filterIsInstance<CfirFunction>().filter { it.cjoDeclarationOrigin != null }.forEach { function ->
                if (functionBody(decl(currentIndex(function))).body == 0u) {
                    assertNull(checkNotNull(function.moduleData.session.importedSourceGraphProvider).sourceGraphOf(function),
                        "合法未选中正文仍为空，不按源文件存在扩大选择")
                }
            }
        }

        private fun currentIndex(declaration: CfirDeclaration): UInt {
            val origin = checkNotNull(declaration.cjoDeclarationOrigin) { "正式库声明必须保留快照来源" }
            assertEquals(snapshot.sourcePath.toAbsolutePath().normalize(), Path.of(origin.packagePath).toAbsolutePath().normalize())
            return (origin.declarationIndex + 1).toUInt()
        }

        private fun declaration(index: UInt, actual: CfirDeclaration) {
            assertEquals(index, currentIndex(actual))
            assertSame(actual, actual.symbol.cfir)
            assertSame(library, actual.moduleData.session)
            val previous = declarations.putIfAbsent(index, actual)
            if (previous != null) {
                assertSame(previous, actual, "同一 d 被拥有槽、FullId 和 TP 重复消费时必须共享声明")
                return
            }
            val wire = decl(index)
            val generic = wire.generic
            if (generic != null) {
                val owner = checkNotNull(actual as? CfirTypeParameterRefsOwner)
                assertEquals(generic.typeParametersLength, owner.typeParameters.size)
                owner.typeParameters.forEachIndexed { ordinal, parameter ->
                    val bound = parameter.symbol.cfir
                    assertSame(actual.symbol, bound.containingDeclarationSymbol, "TP 必须绑定实际声明 owner")
                    declaration(generic.typeParameters(ordinal), bound)
                }
            }
            when (wire.kind) {
                DeclKind.FuncDecl -> {
                    val function = checkNotNull(actual as? CfirFunction)
                    functionHeader(functionBody(wire), function)
                    val body = functionBody(wire).body
                    if (body == 0u) assertFalse(graph.functionBodies.containsKey(function))
                    else expression(body, graph.functionBodies.getValue(function), function)
                }
                DeclKind.FuncParam -> {
                    val parameter = checkNotNull(actual as? CfirValueParameter)
                    type(wire.type, parameter.returnTypeRef.coneType)
                    val default = declInfo(wire, ParamInfo()).defaultVal
                    if (default == 0u) assertFalse(graph.defaults.containsKey(parameter))
                    else expression(default, graph.defaults.getValue(parameter), parameter)
                }
                DeclKind.VarDecl, DeclKind.VarWithPatternDecl -> {
                    val variable = checkNotNull(actual as? CfirVariable)
                    type(wire.type, variable.returnTypeRef.coneType)
                    val initializer = if (wire.kind == DeclKind.VarWithPatternDecl) {
                        val info = declInfo(wire, VarWithPatternInfo())
                        pattern(checkNotNull(info.irrefutablePattern), graph.declarationPatterns.getValue(variable as CfirPatternVariable), variable)
                        info.initializer
                    } else declInfo(wire, VarInfo()).initializer
                    if (initializer == 0u) assertFalse(graph.initializers.containsKey(variable))
                    else expression(initializer, graph.initializers.getValue(variable), variable)
                }
            }
            val declarationPosition = checkNotNull(actual.cjoDeclarationPosition)
            position(wire.begin, declarationPosition.declarationLocation)
            position(wire.end, declarationPosition.endLocation)
            position(wire.identifierPos, declarationPosition.identifierLocation)
        }

        private fun functionHeader(wire: FuncBody, actual: CfirFunction) {
            val parameters = (0 until wire.paramListsLength).flatMap { ordinal ->
                val list = checkNotNull(wire.paramLists(ordinal))
                assertEquals(list.paramsLength, list.desugarsLength, "原 fixture 的对齐参数/helper 槽必须保留")
                (0 until list.paramsLength).map { list.params(it) }
            }
            assertEquals(parameters.size, actual.valueParameters.size)
            actual.valueParameters.forEachIndexed { ordinal, parameter ->
                assertSame(actual.symbol, parameter.containingDeclarationSymbol, "参数不能绑定外层函数或 lambda 的其它 owner")
                declaration(parameters[ordinal], parameter)
            }
            if (wire.retType != 0u) type(wire.retType, actual.returnTypeRef.coneType)
        }

        private fun expression(index: UInt, identity: CfirSourceExpressionIdentity, owner: CfirDeclaration) {
            assertTrue(index > 0u && index <= pkg.allExprsLength.toUInt())
            val previous = expressions.putIfAbsent(index, identity)
            assertSame(owner, graph.expression(identity).context, "e#$index 保留真实拥有域")
            if (previous != null) {
                assertSame(previous, identity, "同一 e 的重复槽及 backedge 必须复用身份")
                return
            }
            assertNull(expressionIndices.put(identity, index), "两个 wire e 不得合并成一个 typed 节点")
            val wire = checkNotNull(pkg.allExprs(index.toInt() - 1))
            val record = graph.expression(identity)
            assertNull(record.span, "导入位置不能通过重读源码伪造 offset")
            val importedPosition = checkNotNull(record.importedPosition)
            position(wire.begin, importedPosition.begin)
            position(wire.end, importedPosition.end)
            val overflow = when (wire.overflowPolicy) {
                OverflowPolicy.NA -> CangjieOverflowStrategy.NA
                OverflowPolicy.Throwing -> CangjieOverflowStrategy.THROWING
                OverflowPolicy.Wrapping -> CangjieOverflowStrategy.WRAPPING
                OverflowPolicy.Saturating -> CangjieOverflowStrategy.SATURATING
                OverflowPolicy.Checked -> CangjieOverflowStrategy.CHECKED
                else -> error("未知官方 overflowPolicy：${wire.overflowPolicy}")
            }
            assertEquals(overflow, record.overflowStrategy)
            type(wire.type, record.type)
            val payload = record.payload
            val kind = when (payload) {
                CfirSourceExpressionPayload.PrimitiveType -> ExprKind.PrimitiveTypeExpr
                CfirSourceExpressionPayload.Wildcard -> ExprKind.WildcardExpr
                is CfirSourceExpressionPayload.Return -> ExprKind.ReturnExpr
                is CfirSourceExpressionPayload.Jump -> ExprKind.JumpExpr
                is CfirSourceExpressionPayload.Reference -> ExprKind.RefExpr
                is CfirSourceExpressionPayload.Member -> ExprKind.MemberAccess
                is CfirSourceExpressionPayload.Call -> ExprKind.CallExpr
                is CfirSourceExpressionPayload.Unary -> ExprKind.UnaryExpr
                is CfirSourceExpressionPayload.Increment -> ExprKind.IncOrDecExpr
                is CfirSourceExpressionPayload.Literal -> ExprKind.LitConstExpr
                is CfirSourceExpressionPayload.Binary -> ExprKind.BinaryExpr
                is CfirSourceExpressionPayload.Subscript -> ExprKind.SubscriptExpr
                is CfirSourceExpressionPayload.Assignment -> ExprKind.AssignExpr
                is CfirSourceExpressionPayload.Array -> ExprKind.ArrayExpr
                is CfirSourceExpressionPayload.Pointer -> ExprKind.PointerExpr
                is CfirSourceExpressionPayload.TypeConversion -> ExprKind.TypeConvExpr
                is CfirSourceExpressionPayload.Throw -> ExprKind.ThrowExpr
                is CfirSourceExpressionPayload.Perform -> ExprKind.PerformExpr
                is CfirSourceExpressionPayload.Spawn -> ExprKind.SpawnExpr
                is CfirSourceExpressionPayload.ArrayLiteral -> ExprKind.ArrayLit
                is CfirSourceExpressionPayload.Tuple -> ExprKind.TupleLit
                is CfirSourceExpressionPayload.Conditional -> ExprKind.IfExpr
                is CfirSourceExpressionPayload.Try -> ExprKind.TryExpr
                is CfirSourceExpressionPayload.Loop -> if (payload.isDoWhile) ExprKind.DoWhileExpr else ExprKind.WhileExpr
                is CfirSourceExpressionPayload.Lambda -> ExprKind.LambdaExpr
                is CfirSourceExpressionPayload.Block -> ExprKind.Block
                is CfirSourceExpressionPayload.Match -> ExprKind.MatchExpr
                is CfirSourceExpressionPayload.LetPatternDestructor -> ExprKind.LetPatternDestructor
                is CfirSourceExpressionPayload.ForIn -> ExprKind.ForInExpr
                is CfirSourceExpressionPayload.MatchCase -> ExprKind.MatchCase
                is CfirSourceExpressionPayload.MatchCaseOther -> ExprKind.MatchCaseOther
                is CfirSourceExpressionPayload.Argument -> ExprKind.InvalidExpr
            }
            assertEquals(wire.kind, kind, "e#$index 必须恢复真实 SOURCE33 角色")
            if (payload is CfirSourceExpressionPayload.Block) {
                val info = exprInfo(wire, BlockInfo())
                assertEquals(wire.operandsLength + 1, info.isExprLength)
                assertEquals(info.isExpr(wire.operandsLength), payload.isUnsafe)
                assertEquals(wire.operandsLength, payload.statements.size)
                payload.statements.forEachIndexed { ordinal, slot ->
                    when (slot) {
                        is CfirSourceSlot.Declaration -> {
                            assertFalse(info.isExpr(ordinal), "Block 槽 $ordinal 是拥有的 d")
                            declaration(wire.operands(ordinal), slot.declaration)
                        }
                        is CfirSourceSlot.Expression -> {
                            assertTrue(info.isExpr(ordinal), "Block 槽 $ordinal 是拥有的 e")
                            expression(wire.operands(ordinal), slot.identity, owner)
                        }
                    }
                }
            } else {
                val slots: List<CfirSourceExpressionIdentity?> = when (payload) {
                    is CfirSourceExpressionPayload.Return -> listOf(payload.value)
                    is CfirSourceExpressionPayload.Member -> listOf(payload.receiver)
                    is CfirSourceExpressionPayload.Call -> listOf(payload.base) + payload.arguments
                    is CfirSourceExpressionPayload.Unary -> listOf(payload.operand)
                    is CfirSourceExpressionPayload.Increment -> listOf(payload.operand)
                    is CfirSourceExpressionPayload.Binary -> listOf(payload.left, payload.right)
                    is CfirSourceExpressionPayload.Subscript -> listOf(payload.receiver) + payload.indices
                    is CfirSourceExpressionPayload.Assignment -> listOf(payload.left, payload.right)
                    is CfirSourceExpressionPayload.Array -> payload.arguments
                    is CfirSourceExpressionPayload.Pointer -> listOfNotNull(payload.argument)
                    is CfirSourceExpressionPayload.TypeConversion -> listOf(payload.value)
                    is CfirSourceExpressionPayload.Throw -> listOf(payload.exception)
                    is CfirSourceExpressionPayload.Perform -> listOf(payload.value)
                    is CfirSourceExpressionPayload.Spawn -> listOfNotNull(payload.argument)
                    is CfirSourceExpressionPayload.ArrayLiteral -> payload.elements
                    is CfirSourceExpressionPayload.Tuple -> payload.elements
                    is CfirSourceExpressionPayload.Conditional -> listOf(payload.condition, payload.thenBranch, payload.elseBranch)
                    is CfirSourceExpressionPayload.Try -> listOf(payload.body) + payload.catches + listOf(payload.finallyBlock)
                    is CfirSourceExpressionPayload.Loop -> if (payload.isDoWhile) listOf(payload.body, payload.condition) else listOf(payload.condition, payload.body)
                    is CfirSourceExpressionPayload.Match -> if (payload.selector == null) payload.cases else listOf(payload.selector) + payload.cases
                    is CfirSourceExpressionPayload.LetPatternDestructor -> listOf(payload.initializer)
                    is CfirSourceExpressionPayload.ForIn -> listOf(payload.iterable, payload.guard, payload.body)
                    is CfirSourceExpressionPayload.MatchCase -> listOf(payload.guard, payload.body)
                    is CfirSourceExpressionPayload.MatchCaseOther -> listOf(payload.condition, payload.body)
                    is CfirSourceExpressionPayload.Argument -> listOfNotNull(payload.value)
                    CfirSourceExpressionPayload.PrimitiveType, CfirSourceExpressionPayload.Wildcard,
                    is CfirSourceExpressionPayload.Jump, is CfirSourceExpressionPayload.Reference,
                    is CfirSourceExpressionPayload.Literal, is CfirSourceExpressionPayload.Lambda -> emptyList()
                    is CfirSourceExpressionPayload.Block -> error("Block 已按 d/e 混合拥有槽核验")
                }
                assertEquals(wire.operandsLength, slots.size, "e#$index 有序槽不能删除、去重或漏掉固定空槽")
                slots.forEachIndexed { ordinal, child ->
                    if (child == null) assertEquals(0u, wire.operands(ordinal))
                    else expression(wire.operands(ordinal), child, owner)
                }
            }
            when (payload) {
                is CfirSourceExpressionPayload.Return -> {
                    assertSame(owner, payload.function, "Return backedge 必须绑定自己的 FuncBody")
                    assertTrue(graph.functionBodies.containsKey(payload.function))
                }
                is CfirSourceExpressionPayload.Jump -> {
                    assertEquals(exprInfo(wire, JumpInfo()).isBreak, payload.isBreak)
                    assertNull(record.mapExpression, "Jump 的 map 槽在正式图中是 loop backedge")
                    expression(wire.mapExpr, payload.loop, owner)
                }
                is CfirSourceExpressionPayload.Reference -> reference(exprInfo(wire, ReferenceInfo()), payload)
                is CfirSourceExpressionPayload.Member -> reference(exprInfo(wire, ReferenceInfo()), payload.reference)
                is CfirSourceExpressionPayload.Call -> {
                    val info = exprInfo(wire, CallInfo())
                    assertEquals(info.hasSideEffect, payload.hasSideEffect)
                    val expectedKind = when (info.callKind) {
                        CallKind.CallDeclaredFunction -> CfirSourceCallKind.DECLARED
                        CallKind.CallIntrinsicFunction -> CfirSourceCallKind.INTRINSIC
                        CallKind.CallSuperFunction -> CfirSourceCallKind.SUPER
                        CallKind.CallObjectCreation -> CfirSourceCallKind.OBJECT_CREATION
                        CallKind.CallStructCreation -> CfirSourceCallKind.STRUCT_CREATION
                        CallKind.CallVariadicFunction -> CfirSourceCallKind.VARIADIC
                        CallKind.CallFunctionPtr -> CfirSourceCallKind.FUNCTION_POINTER
                        CallKind.CallAnnotation -> CfirSourceCallKind.ANNOTATION
                        CallKind.CallBuiltinFunction -> CfirSourceCallKind.BUILTIN
                        else -> error("未知官方 CallKind：${info.callKind}")
                    }
                    assertEquals(expectedKind, payload.kind)
                    payload.arguments.forEach { assertTrue(graph.expression(it).payload is CfirSourceExpressionPayload.Argument) }
                }
                is CfirSourceExpressionPayload.Unary -> operator(exprInfo(wire, UnaryInfo()).op, payload.operator)
                is CfirSourceExpressionPayload.Increment -> operator(exprInfo(wire, IncOrDecInfo()).op, payload.operator)
                is CfirSourceExpressionPayload.Binary -> operator(exprInfo(wire, BinaryInfo()).op, payload.operator)
                is CfirSourceExpressionPayload.Assignment -> {
                    val info = exprInfo(wire, AssignInfo())
                    operator(info.op, payload.operator)
                    assertEquals(info.isCompound, payload.isCompound)
                }
                is CfirSourceExpressionPayload.Literal -> {
                    val info = exprInfo(wire, LitConstInfo())
                    assertEquals(info.strValue, payload.text)
                    val expectedKind = when (info.constKind) {
                        LitConstKind.Integer -> CfirLiteralKind.INT
                        LitConstKind.Float -> CfirLiteralKind.FLOAT
                        LitConstKind.Bool -> CfirLiteralKind.BOOLEAN
                        LitConstKind.Rune -> CfirLiteralKind.RUNE
                        LitConstKind.RuneByte -> CfirLiteralKind.BYTE
                        LitConstKind.String -> CfirLiteralKind.STRING
                        LitConstKind.JString -> CfirLiteralKind.JSTRING
                        LitConstKind.Unit -> CfirLiteralKind.UNIT
                        else -> error("未知官方 literal kind：${info.constKind}")
                    }
                    assertEquals(expectedKind, payload.kind)
                    assertEquals(info.constKind == LitConstKind.JString, payload.isJavaString)
                }
                is CfirSourceExpressionPayload.Subscript -> assertEquals(exprInfo(wire, SubscriptInfo()).isTupleAccess, payload.isTupleAccess)
                is CfirSourceExpressionPayload.ArrayLiteral -> exprInfo(wire, ArrayInfo()).initFunc?.let {
                    target(it, checkNotNull(payload.initializer))
                }
                is CfirSourceExpressionPayload.Lambda -> {
                    val info = exprInfo(wire, LambdaInfo())
                    val function = payload.function
                    assertTrue(function.owner is CfirAnonymousFunction)
                    val anonymous = function.owner as CfirAnonymousFunction
                    assertNotSame(owner, function.owner, "lambda 有自己的 binder")
                    assertTrue(lambdaOwners.add(anonymous), "两个 lambda 不能共享匿名函数身份")
                    assertSame(function.owner, function.owner.symbol.cfir)
                    assertSame(library, function.owner.moduleData.session)
                    assertEquals(info.supportMock, payload.supportMock)
                    assertEquals(info.funcBody.captureKind.toInt(), function.captureKind)
                    assertEquals(info.funcBody.always, function.always)
                    functionHeader(info.funcBody, anonymous)
                    assertEquals(anonymous.valueParameters, function.parameters)
                    val header = checkNotNull(function.importedHeader)
                    assertEquals(info.funcBody.paramListsLength, header.parameterLists.size)
                    assertEquals(info.funcBody.paramListsLength, header.desugaredParameterLists.size)
                    header.parameterLists.forEachIndexed { ordinal, parameters ->
                        val list = checkNotNull(info.funcBody.paramLists(ordinal))
                        assertEquals(list.paramsLength, parameters.size)
                        parameters.forEachIndexed { argument, parameter -> declaration(list.params(argument), parameter) }
                    }
                    if (info.funcBody.retType == 0u) assertNull(header.returnType)
                    else type(info.funcBody.retType, checkNotNull(header.returnType))
                    assertSame(function.owner.returnTypeRef.coneType,
                        assertInstanceOf(CfirSourceCallableSignature.Resolved::class.java, function.signature).returnType)
                    header.desugaredParameterLists.forEachIndexed { ordinal, helpers ->
                        val list = checkNotNull(info.funcBody.paramLists(ordinal))
                        assertEquals(list.desugarsLength, helpers.size)
                        helpers.forEachIndexed { argument, helper ->
                            if (list.desugars(argument) == 0u) assertNull(helper)
                            else declaration(list.desugars(argument), checkNotNull(helper))
                        }
                    }
                    assertSame(function.body, graph.functionBodies.getValue(function.owner))
                    expression(info.funcBody.body, checkNotNull(function.body), function.owner)
                }
                is CfirSourceExpressionPayload.ForIn -> {
                    val info = exprInfo(wire, ForInInfo())
                    pattern(checkNotNull(info.pattern), payload.pattern, owner)
                    val expectedKind = when (info.forInKind) {
                        ForInKind.Range -> CfirSourceForInKind.RANGE
                        ForInKind.String -> CfirSourceForInKind.STRING
                        ForInKind.Iterator -> CfirSourceForInKind.ITERATOR
                        ForInKind.NA -> CfirSourceForInKind.DIRECT
                        else -> error("未知官方 ForInKind：${info.forInKind}")
                    }
                    assertEquals(expectedKind, payload.kind)
                }
                is CfirSourceExpressionPayload.Match -> assertEquals(exprInfo(wire, MatchInfo()).matchMode, payload.selector != null)
                is CfirSourceExpressionPayload.MatchCase -> {
                    val info = exprInfo(wire, MatchCaseInfo())
                    assertEquals(info.patternsLength, payload.patterns.size)
                    payload.patterns.forEachIndexed { ordinal, actual -> pattern(checkNotNull(info.patterns(ordinal)), actual, owner) }
                }
                is CfirSourceExpressionPayload.Try -> {
                    val info = exprInfo(wire, TryInfo())
                    assertEquals(info.patternsLength, payload.patterns.size)
                    payload.patterns.forEachIndexed { ordinal, actual -> pattern(checkNotNull(info.patterns(ordinal)), actual, owner) }
                    assertEquals(info.resourcesLength, payload.resources.size)
                    payload.resources.forEachIndexed { ordinal, resource -> target(checkNotNull(info.resources(ordinal)), resource) }
                }
                is CfirSourceExpressionPayload.Argument -> {
                    val info = exprInfo(wire, FuncArgInfo())
                    assertEquals(info.isDefaultVal, payload.isDefaultValue)
                    assertEquals(info.withInout, payload.withInout)
                }
                else -> Unit // 上方已逐一核验全部33角色和拥有槽；这里仅核验本批 fixture 的附加载荷。
            }
            if (payload !is CfirSourceExpressionPayload.Jump) {
                if (wire.mapExpr == 0u) assertNull(record.mapExpression)
                else expression(wire.mapExpr, checkNotNull(record.mapExpression), owner)
            }
        }

        private fun reference(wire: ReferenceInfo, actual: CfirSourceExpressionPayload.Reference) {
            val spelling = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[actual.name] ?: actual.name.asString()
            assertEquals(wire.reference, spelling, "canonical operator name 与原生保存的运算符拼写使用同一双向名称合同")
            val actualTarget = assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, actual.target).declaration
            target(checkNotNull(wire.target), actualTarget)
            assertEquals(wire.instTysLength, actual.instantiatedTypes.size)
            actual.instantiatedTypes.forEachIndexed { ordinal, argument -> type(wire.instTys(ordinal), argument) }
            if (wire.matchedParentTy == 0u) assertSame(CfirReferenceParentType.Absent, actual.matchedParentType)
            else if (semaType(wire.matchedParentTy).kind == TypeKind.Invalid)
                assertSame(CfirReferenceParentType.Invalid, actual.matchedParentType, "非零 Invalid 不能变为 absent 或错误壳")
            else type(wire.matchedParentTy, checkNotNull(actual.matchedParentType as? CfirReferenceParentType.Instantiated).type)
        }

        private fun target(wire: FullId, actual: CfirDeclaration) {
            assertSame(actual, actual.symbol.cfir)
            if (wire.pkgId == -2) declaration(wire.index, actual)
            else {
                assertTrue(wire.pkgId in 0 until pkg.importsLength)
                assertEquals(0u, wire.index)
                val origin = checkNotNull(actual.cjoDeclarationOrigin)
                val dependency = checkNotNull(CjoExportSourceSnapshots.loadedPackage(library.symbolProvider, Path.of(origin.packagePath)))
                assertEquals(pkg.imports(wire.pkgId), dependency.header.fullPkgName)
                assertEquals(wire.decl, checkNotNull(dependency.pkg.allDecls(origin.declarationIndex)).exportId,
                    "外部 FullId 必须绑定真实 exportId，不能按显示名称重选")
            }
        }

        private fun pattern(wire: Pattern, actual: CfirSourcePattern, owner: CfirDeclaration) {
            val expected = when (actual.kind) {
                CfirSourcePatternKind.CONSTANT -> PatternKind.ConstPattern
                CfirSourcePatternKind.WILDCARD -> PatternKind.WildcardPattern
                CfirSourcePatternKind.VARIABLE -> PatternKind.VarPattern
                CfirSourcePatternKind.TUPLE -> PatternKind.TuplePattern
                CfirSourcePatternKind.TYPE -> PatternKind.TypePattern
                CfirSourcePatternKind.ENUM -> PatternKind.EnumPattern
                CfirSourcePatternKind.EXCEPTION_TYPE -> PatternKind.ExceptTypePattern
                CfirSourcePatternKind.COMMAND_TYPE -> PatternKind.CommandTypePattern
            }
            assertEquals(wire.kind, expected)
            assertEquals(wire.needRuntimeTypeCheck, actual.needRuntimeTypeCheck)
            assertEquals(wire.matchBeforeRuntime, actual.matchBeforeRuntime)
            val hasCapturedTypes = wire.kind == PatternKind.ExceptTypePattern || wire.kind == PatternKind.CommandTypePattern
            assertEquals(if (hasCapturedTypes) 1 else wire.typesLength, actual.types.size)
            actual.types.forEachIndexed { ordinal, argument -> type(wire.types(ordinal), argument) }
            assertEquals(wire.exprsLength, actual.slots.size)
            actual.slots.forEachIndexed { ordinal, slot ->
                when (slot) {
                    is CfirSourceSlot.Declaration -> {
                        assertEquals(PatternKind.VarPattern, wire.kind, "VarPattern.exprs 属于 d 表")
                        declaration(wire.exprs(ordinal), slot.declaration)
                    }
                    is CfirSourceSlot.Expression -> {
                        assertNotEquals(PatternKind.VarPattern, wire.kind)
                        expression(wire.exprs(ordinal), slot.identity, owner)
                    }
                }
            }
            assertEquals(wire.patternsLength, actual.patterns.size)
            actual.patterns.forEachIndexed { ordinal, child -> pattern(checkNotNull(wire.patterns(ordinal)), child, owner) }
            if (wire.kind == PatternKind.ExceptTypePattern || wire.kind == PatternKind.CommandTypePattern) {
                assertEquals(wire.typesLength - 1, actual.typeReferences.size, "捕获 Type 尾槽独立于 pattern 自身语义类型")
                actual.typeReferences.forEachIndexed { ordinal, reference -> type(wire.types(ordinal + 1), reference.coneType) }
            }
            val locations = checkNotNull(actual.importedPosition)
            position(wire.begin, locations.begin)
            position(wire.end, locations.end)
            assertEquals(wire.valuesLength, actual.constantValues.size, "保存的 Pattern.values 不得丢失")
        }

        private fun type(index: UInt, actual: ConeCangJieType) {
            val wire = semaType(index)
            val previous = types.putIfAbsent(index, actual)
            if (previous != null) {
                assertSame(previous, actual, "同一 t 的 header、正文、instTys、pattern 必须复用已完成类型")
                return
            }
            when (wire.kind) {
                TypeKind.Int64 -> assertEquals(ConePrimitiveType.INT64, actual)
                TypeKind.Int32 -> assertEquals(ConePrimitiveType.INT32, actual)
                TypeKind.Bool -> assertEquals(ConePrimitiveType.BOOLEAN, actual)
                TypeKind.Unit -> assertEquals(ConePrimitiveType.UNIT, actual)
                TypeKind.Nothing -> assertEquals(ConePrimitiveType.NOTHING, actual)
                TypeKind.Generic -> {
                    val parameter = checkNotNull(actual as? ConeTypeParameterType).lookupTag.typeParameterSymbol.cfir
                    val info = checkNotNull(wire.info(GenericTyInfo()) as? GenericTyInfo)
                    target(checkNotNull(info.declPtr), parameter)
                    assertEquals(info.upperBoundsLength, parameter.bounds.size, "保留正式 TP 的完整上界")
                    parameter.bounds.forEachIndexed { ordinal, bound -> type(info.upperBounds(ordinal), bound.coneType) }
                }
                TypeKind.Func -> {
                    val function = checkNotNull(actual as? ConeFunctionType)
                    val info = checkNotNull(wire.info(FuncTyInfo()) as? FuncTyInfo)
                    assertEquals(wire.typeArgsLength, function.parameterTypes.size)
                    function.parameterTypes.forEachIndexed { ordinal, argument -> type(wire.typeArgs(ordinal), argument) }
                    type(info.retType, function.returnType)
                }
            }
            if (actual is ConeClassifierType && wire.infoType == SemaTyInfo.CompositeTyInfo) {
                val info = checkNotNull(wire.info(CompositeTyInfo()) as? CompositeTyInfo)
                val id = checkNotNull(info.declPtr)
                if (id.pkgId == -2) {
                    val declaration = decl(id.index)
                    assertEquals(declaration.identifier, actual.lookupTag.classId.shortClassName.asString())
                    assertEquals(pkg.fullPkgName, actual.lookupTag.classId.packageFqName.asString())
                }
                assertEquals(wire.typeArgsLength, actual.typeArguments.size)
                actual.typeArguments.forEachIndexed { ordinal, argument ->
                    type(wire.typeArgs(ordinal), checkNotNull(argument as? ConeCangJieTypeProjection).type)
                }
            }
        }

        private fun position(wire: Position?, actual: CjSourceLocation?) {
            if (wire == null || wire.file == 0u || wire.line <= 0 || wire.column <= 0) {
                assertNull(actual, "官方生成节点的缺失位置不能制造为有效行列")
                return
            }
            assertEquals(0u, wire.pkgId, "本批 SOURCE 位置属于当前包，与 FullId=-2 分别解释")
            val location = checkNotNull(actual)
            assertEquals(wire.line, location.line)
            assertEquals(wire.column, location.column)
            assertEquals(Path.of(checkNotNull(pkg.allFiles(wire.file.toInt() - 1))).toAbsolutePath().normalize(),
                Path.of(location.filePath).toAbsolutePath().normalize())
        }

        /** 固定 native schema 的运算合同；不依赖生产 decoder 的映射表或枚举 ordinal。 */
        private fun operator(wire: UByte, actual: CfirSourceOperator) {
            val expected = when (wire) {
                OperatorKind.Index -> CfirSourceOperator.INDEX
                OperatorKind.Call -> CfirSourceOperator.CALL
                OperatorKind.Not -> CfirSourceOperator.NOT
                OperatorKind.Power -> CfirSourceOperator.POWER
                OperatorKind.Multiply -> CfirSourceOperator.MULTIPLY
                OperatorKind.Divide -> CfirSourceOperator.DIVIDE
                OperatorKind.Remainder -> CfirSourceOperator.REMAINDER
                OperatorKind.Add -> CfirSourceOperator.ADD
                OperatorKind.Subtract -> CfirSourceOperator.SUBTRACT
                OperatorKind.BitLeftShift -> CfirSourceOperator.SHIFT_LEFT
                OperatorKind.BitRightShift -> CfirSourceOperator.SHIFT_RIGHT
                OperatorKind.LT -> CfirSourceOperator.LESS
                OperatorKind.LE -> CfirSourceOperator.LESS_EQUAL
                OperatorKind.GT -> CfirSourceOperator.GREATER
                OperatorKind.GE -> CfirSourceOperator.GREATER_EQUAL
                OperatorKind.Equal -> CfirSourceOperator.EQUAL
                OperatorKind.NotEqual -> CfirSourceOperator.NOT_EQUAL
                OperatorKind.BitAnd -> CfirSourceOperator.BIT_AND
                OperatorKind.BitXor -> CfirSourceOperator.BIT_XOR
                OperatorKind.BitOr -> CfirSourceOperator.BIT_OR
                OperatorKind.PostInc -> CfirSourceOperator.POST_INCREMENT
                OperatorKind.PostDec -> CfirSourceOperator.POST_DECREMENT
                OperatorKind.Is -> CfirSourceOperator.IS
                OperatorKind.As -> CfirSourceOperator.AS
                OperatorKind.LogicAnd -> CfirSourceOperator.LOGICAL_AND
                OperatorKind.LogicOr -> CfirSourceOperator.LOGICAL_OR
                OperatorKind.Coalescing -> CfirSourceOperator.COALESCING
                OperatorKind.Pipeline -> CfirSourceOperator.PIPELINE
                OperatorKind.Composition -> CfirSourceOperator.COMPOSITION
                OperatorKind.Assign -> CfirSourceOperator.ASSIGN
                OperatorKind.PowerAssign -> CfirSourceOperator.POWER_ASSIGN
                OperatorKind.MultiplyAssign -> CfirSourceOperator.MULTIPLY_ASSIGN
                OperatorKind.DivideAssign -> CfirSourceOperator.DIVIDE_ASSIGN
                OperatorKind.RemainderAssign -> CfirSourceOperator.REMAINDER_ASSIGN
                OperatorKind.AddAssign -> CfirSourceOperator.ADD_ASSIGN
                OperatorKind.SubtractAssign -> CfirSourceOperator.SUBTRACT_ASSIGN
                OperatorKind.LeftShiftAssign -> CfirSourceOperator.SHIFT_LEFT_ASSIGN
                OperatorKind.RightShiftAssign -> CfirSourceOperator.SHIFT_RIGHT_ASSIGN
                OperatorKind.BitAndAssign -> CfirSourceOperator.BIT_AND_ASSIGN
                OperatorKind.BitXorAssign -> CfirSourceOperator.BIT_XOR_ASSIGN
                OperatorKind.BitOrAssign -> CfirSourceOperator.BIT_OR_ASSIGN
                OperatorKind.LogicAndAssign -> CfirSourceOperator.LOGICAL_AND_ASSIGN
                OperatorKind.LogicOrAssign -> CfirSourceOperator.LOGICAL_OR_ASSIGN
                else -> error("未知官方 OperatorKind：$wire")
            }
            assertEquals(expected, actual)
        }

        private fun decl(index: UInt): Decl {
            assertTrue(index > 0u && index <= pkg.allDeclsLength.toUInt())
            return checkNotNull(pkg.allDecls(index.toInt() - 1))
        }

        private fun semaType(index: UInt): SemaTy {
            assertTrue(index > 0u && index <= pkg.allTypesLength.toUInt())
            return checkNotNull(pkg.allTypes(index.toInt() - 1))
        }

        private fun functionBody(wire: Decl): FuncBody = checkNotNull(declInfo(wire, FuncInfo()).funcBody)
        private fun <T : Table> declInfo(wire: Decl, table: T): T {
            assertNotNull(wire.info(table))
            return table
        }
        private fun <T : Table> exprInfo(wire: Expr, table: T): T {
            assertNotNull(wire.info(table))
            return table
        }
    }
}

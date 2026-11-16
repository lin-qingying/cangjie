package org.cangnova.cangjie.lsp.semantic

import kotlin.random.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.macro.expansion.FakeMacroSemanticExpansionEngine
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.analysis.LspMacroExpansionOptions
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.cangnova.cangjie.macro.expansion.MacroExpansionArtifactPreparation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.config.CompilerConfiguration
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SemanticTokensRangeParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 语义 token 的编码契约测试。
 *
 * LSP 的语义 token 采用**相对编码**：每 5 个整数一组，`deltaLine`/`deltaStart` 相对上一
 * token。编码错一格不会报错，只会让整片代码着色错位，因此这里把编码不变量全部压成断言：
 * 组长度、非负增量、长度为正、type/modifier 落在 legend 内、token 不重叠、不越界。
 *
 * 另外用固定种子随机取子范围，验证「范围请求是全文请求的子集」——这条一旦破坏，
 * 编辑器局部重绘就会与整篇渲染不一致。
 */
class CangJieSemanticTokenEncodingTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：需要带真实源码的临时工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 全文语义 token 必须满足全部编码不变量。
     */
    @Test
    fun fullDocumentTokensSatisfyDeltaEncodingInvariants() {
        workspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/model.cj")
                val text = fixture.text("src/model.cj")
                session.openDocument(uri, text)

                val legend = requireNotNull(
                    session.initializeResult().capabilities.semanticTokensProvider?.legend,
                ) { "语义 token 能力已宣告，legend 不得为空" }

                val tokens = session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(uri)))
                assertFalse(tokens.data.isEmpty(), "真实源码必须产出语义 token")

                val decoded = decode(tokens)
                assertEncodedInvariants(tokens, decoded, legend.tokenTypes.size, legend.tokenModifiers.size, text)
            }
        }
    }

    /** LSP 引用 token 与 IDE 使用同一份 Analysis API 符号分类。 */
    @Test
    fun resolvedReferencesUseSharedSemanticKinds() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(
                "semantic.cj",
                """
                package sample.semantic

                class TokenBox {
                    private var stored: Int64 = 1
                    public static var staticStored: Int64 = 2

                    public static func readStatic(): Int64 {
                        return staticStored
                    }

                    public mut static prop staticValue: Int64 {
                        get() { return staticStored }
                        set(v) { staticStored = v }
                    }

                    public mut prop value: Int64 {
                        get() { return stored }
                        set(v) { stored = v }
                    }

                    public prop callable: (Int64) -> Int64 {
                        get() { return identity }
                    }

                public func readStored(): Int64 {
                    return stored
                }
                }

            extend TokenBox {
                public prop extensionValue: Int64 {
                    get() { return value }
                }

                public func extended(): Int64 {
                    return value
                }
            }

                struct TokenStruct {
                    let amount: Int64 = 2
                }

                struct TokenOperator {
                    public operator func +(right: TokenOperator): TokenOperator {
                        return right
                    }
                }

            abstract class SemanticAbstract {
            }

            type SemanticAlias = TokenBox

                func makeBox(): TokenBox {
                    return TokenBox()
                }

            func identity(value: Int64): Int64 {
                return value
            }

            func consumeAbstract(value: SemanticAbstract): Int64 {
                return 0
            }

            func optionalAccess(maybe: ?TokenBox): ?Int64 {
                return maybe?.value
            }

            func useOperator(): TokenOperator {
                return TokenOperator() + TokenOperator()
            }

                func useSymbols(): Int64 {
                    let box = makeBox()
                    let staticRead = TokenBox.readStatic()
                    let staticFieldValue = TokenBox.staticStored
                    let staticPropertyValue = TokenBox.staticValue
                    let instanceValue = box.value
                    let callablePropertyValue = box.callable(instanceValue)
                    let structure: TokenStruct = TokenStruct()
                let alias: SemanticAlias = TokenBox()
                let callback: (Int64) -> Int64 = identity
                let callbackValue = callback(instanceValue)
                let methodValue = box.readStored()
                let extensionValue = box.extended()
                let extensionPropertyValue = box.extensionValue
                let argumentValue = identity(value: methodValue)
                    return staticRead + staticFieldValue + staticPropertyValue + callablePropertyValue + callbackValue + argumentValue + extensionValue + extensionPropertyValue +
                    structure.amount + alias.value
                }
                """.trimIndent(),
            )
            .build()

        fixture.use {
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/semantic.cj")
                val text = fixture.text("src/semantic.cj")
                session.openDocument(uri, text)

                val legend = requireNotNull(
                    session.initializeResult().capabilities.semanticTokensProvider?.legend,
                )
                val decoded = decode(session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(uri))))
                val lines = text.lineSequence().toList()

                fun textsFor(typeName: String): List<String> {
                    val typeIndex = legend.tokenTypes.indexOf(typeName)
                    assertTrue(typeIndex >= 0, "LSP legend must include $typeName")
                    return decoded.filter { it.type == typeIndex }.map { token ->
                        lines[token.line].substring(token.start, token.start + token.length)
                    }
                }

                fun hasModifier(typeName: String, tokenText: String, modifierName: String): Boolean {
                    val typeIndex = legend.tokenTypes.indexOf(typeName)
                    val modifierIndex = legend.tokenModifiers.indexOf(modifierName)
                    assertTrue(typeIndex >= 0, "LSP legend must include $typeName")
                    assertTrue(modifierIndex >= 0, "LSP legend must include $modifierName")
                    val modifierMask = 1 shl modifierIndex
                    return decoded.any { token ->
                        token.type == typeIndex &&
                            lines[token.line].substring(token.start, token.start + token.length) == tokenText &&
                            (token.modifiers and modifierMask) != 0
                    }
                }

                assertTrue(
                    textsFor("property").count { it == "value" } >= 2,
                    "property tokens=${textsFor("property")}; source=$text",
                )
                assertTrue(
                    textsFor("variable").count { it == "box" } >= 2,
                    "variable tokens=${textsFor("variable")}",
                )
                assertTrue(
                    textsFor("variable").count { it == "stored" } >= 4,
                    "field read and write references must share the instance-variable token: " +
                        decoded.filter { token ->
                            token.type == legend.tokenTypes.indexOf("variable") &&
                                lines[token.line].substring(token.start, token.start + token.length) == "stored"
                        }.map { token -> "${token.line}:${token.start} ${lines[token.line]}" },
                )
                val callableTokenTypes = setOf(legend.tokenTypes.indexOf("function"), legend.tokenTypes.indexOf("method"))
                assertTrue(
                    decoded.any { token ->
                        token.type in callableTokenTypes &&
                            lines[token.line].substring(token.start, token.start + token.length) == "+"
                    },
                    "successfully resolved operator calls must use a callable semantic token",
                )
                assertTrue(textsFor("class").count { it == "TokenBox" } >= 2, "class tokens=${textsFor("class")}")
                assertTrue(textsFor("struct").count { it == "TokenStruct" } >= 2, "struct tokens=${textsFor("struct")}")
                assertTrue(textsFor("type").contains("Int64"), "primitive type references must use the shared type token")
                assertTrue(hasModifier("type", "Int64", "basicType"), "primitive references need their own semantic category")
                assertTrue(textsFor("method").contains("readStored"), "member call must use the LSP method token")
                assertTrue(textsFor("method").contains("readStatic"), "static member call must use the LSP method token")
                assertTrue(textsFor("method").contains("extended"), "extension call must use the LSP method token")
                assertTrue(
                    textsFor("variable").count { it == "callback" } >= 2,
                    "function-value calls must retain the called variable token",
                )
                assertTrue(
                    textsFor("property").count { it == "callable" } >= 2,
                    "function-value calls through properties must retain the property token",
                )
                assertTrue(textsFor("parameter").contains("value"), "named arguments must use the LSP parameter token")
                assertTrue(hasModifier("method", "extended", "extension"), "extension calls must retain their category")
                assertTrue(hasModifier("method", "readStatic", "static"), "static member calls need an LSP modifier")
                val optionalAccessLineIndex = lines.indexOfFirst { line -> "return maybe?.value" in line }
                assertTrue(optionalAccessLineIndex >= 0, "optionalAccess fixture line is missing")
                val optionalAccessLine = lines[optionalAccessLineIndex]
                val optionalAccessQuestionStart = optionalAccessLine.indexOf("?.")
                val operatorTypeIndex = legend.tokenTypes.indexOf("operator")
                val safeAccessModifierIndex = legend.tokenModifiers.indexOf("safeAccess")
                assertTrue(operatorTypeIndex >= 0, "LSP legend must include operator tokens")
                assertTrue(safeAccessModifierIndex >= 0, "LSP legend must include the safeAccess modifier")
                val safeAccessModifierMask = 1 shl safeAccessModifierIndex
                assertTrue(
                    decoded.any { token ->
                        token.type == operatorTypeIndex &&
                            token.line == optionalAccessLineIndex &&
                            token.start == optionalAccessQuestionStart &&
                            token.length == 1 &&
                            token.modifiers and safeAccessModifierMask != 0
                    },
                    "the optional-chain question mark must carry the safeAccess modifier; " +
                        "lineTokens=${decoded.filter { token -> token.line == optionalAccessLineIndex }.map { token ->
                            val tokenText = optionalAccessLine.substring(token.start, token.start + token.length)
                            "${legend.tokenTypes[token.type]}:$tokenText modifiers=${token.modifiers}"
                        }}",
                )
                assertTrue(hasModifier("variable", "callback", "functionValue"), "function-value calls need an LSP modifier")
                assertTrue(hasModifier("variable", "callback", "readonly"), "function-value calls retain variable mutability")
                assertTrue(hasModifier("property", "callable", "readonly"), "property calls retain read-only state")
                assertTrue(
                    hasModifier("function", "makeBox", "package"),
                    "top-level calls need an LSP modifier",
                )
                assertTrue(hasModifier("class", "TokenBox", "constructor"), "class constructor calls need an LSP modifier")
                assertTrue(
                    textsFor("type").contains("SemanticAbstract"),
                    "Abstract-class references must use the shared user-type token category",
                )
                assertFalse(
                    hasModifier("type", "SemanticAbstract", "abstract"),
                    "Type-reference tokens must not inherit declaration-only abstract modifiers",
                )
                assertTrue(
                    textsFor("type").contains("SemanticAlias"),
                    "Type-alias references must use the shared user-type token category",
                )
                assertFalse(
                    hasModifier("type", "SemanticAlias", "typeAlias"),
                    "Type-alias declaration modifiers must not leak onto type references",
                )
                assertTrue(hasModifier("variable", "stored", "field"), "field references need an LSP modifier")
                assertTrue(hasModifier("variable", "staticStored", "field"), "static fields need an LSP field modifier")
                assertTrue(hasModifier("variable", "staticStored", "static"), "static fields need an LSP static modifier")
                assertTrue(hasModifier("property", "staticValue", "static"), "static properties need an LSP static modifier")
                assertTrue(hasModifier("property", "extensionValue", "extension"), "extension properties need an LSP modifier")
            }
        }
    }

    /** Annotation and macro invocation tokens require a unique successful Analysis API target. */
    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun resolvedAnnotationAndMacroCallsUseDedicatedSemanticKinds() {
        val macroResolutionFinished = CountDownLatch(1)
        val macroEngine = FakeMacroSemanticExpansionEngine().apply {
            preparation = MacroExpansionArtifactPreparation(
                definitions = listOf(
                    MacroDefinitionEntry(
                        packageFqName = FqName("sample.macros"),
                        name = Name.identifier("MakeInt64"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        supportsPlainAttrOverload = true,
                    ),
                    MacroDefinitionEntry(
                        packageFqName = FqName("sample.macros"),
                        name = Name.identifier("AliasMacro"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        supportsPlainAttrOverload = true,
                    ),
                    MacroDefinitionEntry(
                        packageFqName = FqName("sample.firstMacros"),
                        name = Name.identifier("AmbiguousMacro"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        supportsPlainAttrOverload = true,
                    ),
                    MacroDefinitionEntry(
                        packageFqName = FqName("sample.secondMacros"),
                        name = Name.identifier("AmbiguousMacro"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        supportsPlainAttrOverload = true,
                    ),
                ),
            )
        }
        val macroOptions = LspMacroExpansionOptions(
            engine = macroEngine,
            compilerConfigurationProvider = { _, _, _ -> CompilerConfiguration() },
            externalMacroPackagesProvider = {
                setOf(
                    FqName("sample.macros"),
                    FqName("sample.firstMacros"),
                    FqName("sample.secondMacros"),
                )
            },
            abandonedListener = { _, _ -> macroResolutionFinished.countDown() },
        )
        val serverOptions = CangjieLspServerOptions(
            environmentMode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = macroOptions,
            environmentFactory = {
                ServiceLoadedLspTestEnvironment.create(
                    mode = CangJieCoreEnvironmentMode.UnitTest,
                    macroExpansion = macroOptions,
                )
            },
            exitHandler = {},
        )
        val fixture = LspWorkspaceFixtureBuilder()
            .source(
                "annotations/version_tag.cj",
                """
                package sample.version

                @Annotation[target: [Parameter]]
                public class VersionTag {
                    public const init(code!: String) {}
                }
                """.trimIndent(),
            )
            .source(
                "annotations/first_collision_tag.cj",
                """
                package sample.first

                @Annotation[target: [Parameter]]
                public class CollisionTag {
                    public const init(code!: String) {}
                }
                """.trimIndent(),
            )
            .source(
                "annotations/second_collision_tag.cj",
                """
                package sample.second

                @Annotation[target: [Parameter]]
                public class CollisionTag {
                    public const init(code!: String) {}
                }
                """.trimIndent(),
            )
            .source(
                "calls.cj",
                """
                import sample.macros.MakeInt64
                import sample.macros.AliasMacro as RenamedMacro
                import sample.firstMacros.AmbiguousMacro
                import sample.secondMacros.AmbiguousMacro
                import sample.version.VersionTag
                import sample.first.CollisionTag
                import sample.second.CollisionTag

                @Deprecated[message: "legacy API"]
                func deprecatedApi(): Int64 { return 0 }

                func parameterAnnotation(@Deprecated[message: "parameter API"] value: Int64): Int64 { return value }
                func customAnnotation(@VersionTag[code: "1.0"] value: Int64): Int64 { return value }
                func ambiguousAnnotation(@CollisionTag[code: "x"] value: Int64): Int64 { return value }

                func consume(message: String): Unit {}
                func ordinaryNamedArgument(): Unit { consume(message: "ordinary") }

                func macroCall(): Int64 { return @MakeInt64() }
                func ambiguousMacroCall(): Unit { @AmbiguousMacro() }
                func renamedMacroCall(): Unit { @RenamedMacro() }
                func originalNameAfterAlias(): Unit { @AliasMacro() }

                func unresolvedAnnotation(@MissingAnnotation[message: "unresolved"] value: Int64): Int64 { return value }

                @MissingCustomAnnotation
                func unresolvedCustomAnnotation(): Unit {}

                func unresolvedMacro(): Unit { @MissingMacro() }
                func unresolvedMacroEscape(): Unit { @MissingMacro(\@) }
                """.trimIndent(),
            )
            .build()

        fixture.use {
            createSession(fixture.initializeParams(), serverOptions).use { session ->
                val uri = fixture.uri("src/calls.cj")
                val text = fixture.text("src/calls.cj")
                session.openDocument(uri, text)
                session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(uri)))
                assertTrue(
                    macroResolutionFinished.await(20, TimeUnit.SECONDS),
                    "Macro resolution must finish before asserting its semantic token.",
                )

                val legend = requireNotNull(
                    session.initializeResult().capabilities.semanticTokensProvider?.legend,
                )
                val decoded = decode(session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(uri))))
                val lines = text.lineSequence().toList()

                fun hasToken(tokenType: String, tokenText: String, context: String): Boolean {
                    val typeIndex = legend.tokenTypes.indexOf(tokenType)
                    assertTrue(typeIndex >= 0, "LSP legend must include $tokenType")
                    val contextStart = text.indexOf(context)
                    assertTrue(contextStart >= 0, "Missing source context `$context`")
                    val tokenOffset = text.indexOf(tokenText, contextStart)
                    assertTrue(tokenOffset >= contextStart, "`$tokenText` is not in `$context`")
                    val tokenLine = text.substring(0, tokenOffset).count { char -> char == '\n' }
                    val tokenStart = tokenOffset - (text.lastIndexOf('\n', tokenOffset - 1) + 1)
                    return decoded.any { token ->
                        token.type == typeIndex &&
                            token.line == tokenLine &&
                            token.start == tokenStart &&
                            lines[token.line].substring(token.start, token.start + token.length) == tokenText
                    }
                }

                fun hasModifier(tokenType: String, tokenText: String, context: String, modifierName: String): Boolean {
                    val typeIndex = legend.tokenTypes.indexOf(tokenType)
                    val modifierIndex = legend.tokenModifiers.indexOf(modifierName)
                    assertTrue(typeIndex >= 0, "LSP legend must include $tokenType")
                    assertTrue(modifierIndex >= 0, "LSP legend must include $modifierName")
                    val contextStart = text.indexOf(context)
                    val tokenOffset = text.indexOf(tokenText, contextStart)
                    val tokenLine = text.substring(0, tokenOffset).count { char -> char == '\n' }
                    val tokenStart = tokenOffset - (text.lastIndexOf('\n', tokenOffset - 1) + 1)
                    val modifierMask = 1 shl modifierIndex
                    return decoded.any { token ->
                        token.type == typeIndex &&
                            token.line == tokenLine &&
                            token.start == tokenStart &&
                            (token.modifiers and modifierMask) != 0
                    }
                }

                assertTrue(
                    hasToken("decorator", "Deprecated", "@Deprecated[message:"),
                    "Resolved annotation names must use the decorator token type; " +
                        "tokens=${decoded.map { token ->
                            val tokenText = lines[token.line].substring(token.start, token.start + token.length)
                            "${legend.tokenTypes[token.type]}:$tokenText"
                        }}",
                )
                assertTrue(
                    hasModifier("parameter", "message", "@Deprecated[message:", "annotationAttribute"),
                    "Resolved annotation argument names must carry the annotationAttribute modifier",
                )
                assertTrue(
                    hasModifier("parameter", "message", "parameterAnnotation(@Deprecated[message:", "annotationAttribute"),
                    "Resolved parameter annotations must carry the annotationAttribute modifier",
                )
                assertTrue(
                    hasToken("decorator", "VersionTag", "customAnnotation(@VersionTag[code:"),
                    "Resolved custom annotation names must use the decorator token type; " +
                        "tokens=${decoded.map { token ->
                            val tokenText = lines[token.line].substring(token.start, token.start + token.length)
                            "${legend.tokenTypes[token.type]}:$tokenText"
                        }}",
                )
                assertTrue(
                    hasModifier("parameter", "code", "customAnnotation(@VersionTag[code:", "annotationAttribute"),
                    "Resolved custom annotation constructor parameters must carry annotationAttribute",
                )
                assertTrue(
                    hasToken("parameter", "message", "ordinaryNamedArgument(): Unit { consume(message:"),
                    "Ordinary named arguments must retain the parameter token type",
                )
                assertTrue(
                    hasToken("type", "String", "func consume(message: String)"),
                    "String is a user-defined type reference",
                )
                assertFalse(
                    hasModifier(
                        "parameter",
                        "message",
                        "ordinaryNamedArgument(): Unit { consume(message:",
                        "annotationAttribute",
                    ),
                    "Ordinary named arguments must not receive annotationAttribute",
                )
                assertFalse(
                    hasModifier("type", "String", "func consume(message: String)", "basicType"),
                    "String references must not receive the basic-type modifier",
                )
                assertTrue(
                    hasToken("macro", "MakeInt64", "return @MakeInt64()"),
                    "Resolved macro invocation names must use the macro token type",
                )
                assertTrue(
                    hasModifier("macro", "\\@", "unresolvedMacroEscape(): Unit { @MissingMacro(", "macroEscape"),
                    "Macro token escapes must be highlighted from macro input PSI even when the macro name is unresolved",
                )
                assertFalse(
                    hasToken("macro", "AmbiguousMacro", "@AmbiguousMacro()"),
                    "Ambiguous macro imports must not receive the resolved macro token",
                )
                assertTrue(
                    hasToken("macro", "RenamedMacro", "@RenamedMacro()"),
                    "A macro alias must resolve using the imported alias name",
                )
                assertFalse(
                    hasToken("macro", "AliasMacro", "@AliasMacro()"),
                    "The original macro name must not remain visible after importing an alias",
                )
                assertFalse(
                    hasToken("decorator", "CollisionTag", "ambiguousAnnotation(@CollisionTag[code:"),
                    "Ambiguous annotation imports must not receive the decorator token",
                )
                assertFalse(
                    hasModifier("parameter", "code", "ambiguousAnnotation(@CollisionTag[code:", "annotationAttribute"),
                    "Ambiguous annotation parameters must not receive annotationAttribute",
                )
                assertFalse(
                    hasToken("decorator", "MissingAnnotation", "unresolvedAnnotation(@MissingAnnotation[message:"),
                    "Unresolved annotation names must not receive the resolved annotation token",
                )
                assertFalse(
                    hasToken("decorator", "MissingCustomAnnotation", "@MissingCustomAnnotation"),
                    "An unresolved custom annotation wrapper must not receive the decorator token",
                )
                assertFalse(
                    hasModifier("parameter", "message", "unresolvedAnnotation(@MissingAnnotation[message:", "annotationAttribute"),
                    "Unresolved annotation parameters must not receive the annotationAttribute modifier",
                )
                assertFalse(
                    hasToken("macro", "MissingMacro", "@MissingMacro()"),
                    "Unresolved macro names must not receive the macro token",
                )
            }
        }
    }

    /**
     * 范围请求返回的 token 必须是全文请求的子集（按绝对坐标比较）。
     */
    @Test
    fun `rangeTokensAreSubsetOfFullDocumentTokens`() {
        workspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/model.cj")
                val text = fixture.text("src/model.cj")
                session.openDocument(uri, text)

                val full = decode(session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(uri))))
                    .map { it.absoluteKey() }
                    .toSet()
                val lineCount = text.lineSequence().count()

                val lines = text.lineSequence().toList()
                val random = Random(SEED)
                repeat(RANGE_CASES) { case ->
                    val firstLine = random.nextInt(lineCount)
                    val lastLine = random.nextInt(firstLine, lineCount.coerceAtLeast(firstLine + 1))
                    val range = Range(
                        Position(firstLine, 0),
                        Position(lastLine, lines[lastLine.coerceAtMost(lines.lastIndex)].length),
                    )

                    val rangeTokens = decode(
                        session.semanticTokensRange(
                            SemanticTokensRangeParams(TextDocumentIdentifier(uri), range),
                        ),
                    )

                    rangeTokens.forEach { token ->
                        assertTrue(
                            token.absoluteKey() in full,
                            "case=$case 范围 ${range.start.line}..${range.end.line} 的 token " +
                                "${token.absoluteKey()} 不在全文结果里：局部重绘会与整篇渲染不一致",
                        )
                    }
                }
            }
        }
    }

    /**
     * 断言编码不变量。
     */
    private fun assertEncodedInvariants(
        tokens: SemanticTokens,
        decoded: List<DecodedToken>,
        legendTypes: Int,
        legendModifiers: Int,
        text: String,
    ) {
        assertEquals(0, tokens.data.size % 5, "语义 token 数据必须是 5 元组序列")
        val lineCount = text.lineSequence().count()

        decoded.forEachIndexed { index, token ->
            assertTrue(token.line in 0 until lineCount, "第 $index 个 token 行号越界：${token.line}")
            assertTrue(token.length > 0, "第 $index 个 token 长度必须为正：$token")
            assertTrue(token.type in 0 until legendTypes, "第 $index 个 token 的类型不在 legend 内：${token.type}")
            assertTrue(
                token.modifiers == 0 || token.modifiers ushr legendModifiers == 0,
                "第 $index 个 token 的修饰符位图超出 legend 声明的 ${legendModifiers} 项：${token.modifiers}",
            )

            val previous = decoded.getOrNull(index - 1)
            if (previous != null && previous.line == token.line) {
                assertTrue(
                    previous.start + previous.length <= token.start,
                    "第 $index 个 token 与前一个重叠：$previous -> $token",
                )
            }
        }
    }

    /**
     * 解码相对编码为绝对坐标。
     */
    private fun decode(tokens: SemanticTokens): List<DecodedToken> {
        var line = 0
        var start = 0
        return tokens.data.chunked(5).map { group ->
            val deltaLine = group[0]
            val deltaStart = group[1]
            line += deltaLine
            start = if (deltaLine == 0) start + deltaStart else deltaStart
            DecodedToken(
                line = line,
                start = start,
                length = group[2],
                type = group[3],
                modifiers = group[4],
            )
        }
    }

    /**
     * 两文件语义工作区（与语义能力用例同一形态，保证 token 有跨文件引用可着色）。
     */
    private fun workspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .source(
            "model.cj",
            """
            package sample.semantic

            interface Base {
                func code(): Int64
            }

            class User <: Base {
                override func code(): Int64 {
                    return 1
                }
            }

            func buildUser(): User {
                return User()
            }
            """.trimIndent(),
        )
        .source(
            "use.cj",
            """
            package sample.semantic

            func consume(): Int64 {
                let user = buildUser()
                return user.code()
            }
            """.trimIndent(),
        )
        .build()

    /**
     * 解码后的绝对坐标 token。
     */
    private data class DecodedToken(
        val line: Int,
        val start: Int,
        val length: Int,
        val type: Int,
        val modifiers: Int,
    ) {
        /** 跨全文/范围比较用的绝对键。 */
        fun absoluteKey(): String = "$line:$start:$length:$type:$modifiers"
    }

    companion object {
        /** 固定种子。 */
        private const val SEED: Int = 20261111

        /** 随机子范围用例数。 */
        private const val RANGE_CASES: Int = 8
    }
}

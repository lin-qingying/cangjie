package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

/**
 * 官方 v1.1.3 的五项 CJMP 声明语法约束必须由共享 parser 报告；alpha.20 的独立版本规则另行门控。
 * 消息与范围按官方规则独立指定；PSI 与 LightTree 分别和期望比较，不能互为正确性依据。
 */
class CjmpDeclarationRulesParsingTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /**
     * HasDefault 使用有效 SPECIFIC 属性；无体 specific 函数和属性可豁免。
     * 接口变量缺少修饰符规则表，specific let/var 的有效角色清空，仍须按初始化值判断实现。
     * legacy-scope-expectations/cjc-results.json 的 interface-field 同时报缺实现和变量作用域错误。
     */
    @Test
    fun specificInterfaceChecksDirectMembersAndSpecificExemption() {
        val members = listOf(
            "func missing(): Unit" to listOf("missing"),
            "prop missing: Int64" to listOf("missing"),
            "let missing: Int64" to listOf("missing"),
            "var missing: Int64" to listOf("missing"),
            "specific func missing(): Unit" to emptyList(),
            "specific prop missing: Int64" to emptyList(),
            "specific let missing: Int64" to listOf("missing"),
            "specific var missing: Int64" to listOf("missing"),
            "func missing(): Unit {}" to emptyList(),
            "prop missing: Int64 { get() { 1 } }" to emptyList(),
            "mut prop missing: Int64 { set(value) {} }" to emptyList(),
            "let missing: Int64 = 1" to emptyList(),
            "var missing: Int64 = 1" to emptyList(),
        )
        for ((member, names) in members) {
            val text = "specific interface I {\n$member\n}"
            assertBoth(text, SPECIFIC, names.map {
                range(text, member, "the member $it must have body in 'specific' I")
            })
        }
        val text = "specific interface I {\nfunc implemented(): Unit { let local: Int64 }\n}"
        assertBoth(text, SPECIFIC, emptyList())
    }

    /** HasDefault 按 accessor 列表判断属性；accessor 自身缺体或带可恢复的语法错误不改变该事实。 */
    @Test
    fun specificInterfaceRecognizesPropertyAccessorsAndInitializers() {
        val accessorWithoutBody = "prop value: Int64 { get() }"
        val accessorWithoutBodyText = "specific interface I {\n$accessorWithoutBody\n}"
        assertBoth(accessorWithoutBodyText, SPECIFIC, emptyList(), rulesOnly = true)

        for (accessor in listOf(
            // v1.1.3 rejects annotations on getters, but still records the accessor before reporting it.
            "@Deprecated[message: \"old\"] get() { 1 }",
            // Deprecated is the setter annotation exception; other setter annotations are rejected.
            "@Deprecated[message: \"old\"] set(value) {}",
            "@Frozen set(value) {}",
            // Accessor modifiers are diagnosed, but do not prevent accessor-list registration.
            "public get() { 1 }",
            "private set(value) {}",
        )) {
            val property = "prop value: Int64 { $accessor }"
            val text = "specific interface I {\n$property\n}"
            assertBoth(text, SPECIFIC, emptyList(), rulesOnly = true)
        }

        // HasDefault uses the parsed initializer even when its expression is an error node.
        val failedInitializer = "let value: Int64 ="
        val failedInitializerText = "specific interface I {\n$failedInitializer\n}"
        assertBoth(failedInitializerText, SPECIFIC, emptyList(), rulesOnly = true)

        for (member in listOf(
            "const constant: Int64 = 1",
            "foreign let foreignValue: Int64 = 1",
            "unsafe let unsafeValue: Int64 = 1",
        )) {
            val text = "specific interface I {\n$member\n}"
            assertBoth(text, SPECIFIC, emptyList(), rulesOnly = true)
        }
    }

    /** accessor 前缀按官方语法保留自身诊断；只有 setter 的 Deprecated 注解是例外。 */
    @Test
    fun propertyAccessorAnnotationsAndModifiersFollowOfficialRules() {
        val getterAnnotation = "@Deprecated[message: \"old\"]"
        val getter = "$getterAnnotation get() { 1 }"
        val getterText = "specific interface I { prop value: Int64 { $getter } }"
        assertBoth(
            getterText,
            SPECIFIC,
            listOf(range(
                getterText,
                getterAnnotation,
                "unexpected annotation '@Deprecated' on get",
                hints = listOf(hintAfterToken(getterText, "get", 3, "on get")),
            )),
            accessorSyntaxOnly = true,
        )

        val allowedSetterAnnotation = "@Deprecated[message: \"old\"]"
        val allowedSetter = "prop value: Int64 { $allowedSetterAnnotation set(value) {} }"
        assertBoth("specific interface I { $allowedSetter }", SPECIFIC, emptyList(), accessorSyntaxOnly = true)

        val setterAnnotation = "@Frozen"
        val setter = "$setterAnnotation set(value) {}"
        val setterProperty = "prop value: Int64 { $setter }"
        val setterText = "specific interface I { $setterProperty }"
        assertBoth(
            setterText,
            SPECIFIC,
            listOf(range(
                setterText,
                setterAnnotation,
                "unexpected annotation '@Frozen' on set",
                hints = listOf(hintAfterToken(setterText, "set", 3, "on set")),
            )),
            accessorSyntaxOnly = true,
        )

        for (modifier in listOf("private", "static", "common", "specific", "const", "foreign", "unsafe", "inout")) {
            val accessor = "$modifier get() {}"
            val property = "prop value: Int64 { $accessor }"
            val text = "specific interface I { $property }"
            val accessorDescription = CangJieParsingBundle.message("parsing.accessor.getter.function").toString()
            assertBoth(
                text,
                SPECIFIC,
                listOf(ParsingDiagnostic(
                    startOffset = text.lastIndexOf(modifier),
                    endOffset = text.lastIndexOf(modifier) + modifier.length,
                    message = "expected no modifier before $accessorDescription, found '$modifier'",
                    hints = listOf(hintAfterToken(
                        text,
                        "get",
                        3,
                        CangJieParsingBundle.message("parsing.error.hint.before", accessorDescription).toString(),
                    )),
                )),
                accessorSyntaxOnly = true,
            )
        }

        val setterModifier = "private set(value) {}"
        val setterPropertyWithModifier = "prop value: Int64 { $setterModifier }"
        val setterModifierText = "specific interface I { $setterPropertyWithModifier }"
        val setterDescription = CangJieParsingBundle.message("parsing.accessor.setter.function").toString()
        assertBoth(
            setterModifierText,
            SPECIFIC,
            listOf(range(
                setterModifierText,
                "private",
                "expected no modifier before $setterDescription, found 'private'",
                hints = listOf(hintAfterToken(
                    setterModifierText,
                    "set",
                    3,
                    CangJieParsingBundle.message("parsing.error.hint.before", setterDescription).toString(),
                )),
            )),
            accessorSyntaxOnly = true,
        )

        val modifiers = "public static"
        val accessor = "$modifiers get() {}"
        val property = "prop value: Int64 { $accessor }"
        val text = "specific interface I { $property }"
        assertBoth(
            text,
            SPECIFIC,
            listOf(
                range(
                    text,
                    "static",
                    "expected no modifier before getter function, found 'static'",
                    hints = listOf(hintAfterToken(text, "get", 3, "before getter function")),
                ),
            ),
            accessorSyntaxOnly = true,
        )

        val annotatedSetterModifier = "@Frozen private set(value) {}"
        val annotatedSetterProperty = "prop value: Int64 { $annotatedSetterModifier }"
        val annotatedSetterText = "specific interface I { $annotatedSetterProperty }"
        val annotationStart = annotatedSetterText.lastIndexOf("@Frozen")
        val modifierStart = annotatedSetterText.lastIndexOf("private")
        val setterStart = annotatedSetterText.lastIndexOf("set")
        assertBoth(
            annotatedSetterText,
            SPECIFIC,
            listOf(
                ParsingDiagnostic(
                    annotationStart,
                    annotationStart + "@Frozen".length,
                    "unexpected annotation '@Frozen' on set",
                    hints = listOf(ParsingDiagnosticHint(
                        modifierStart,
                        modifierStart + 3,
                        "on set",
                    )),
                ),
                ParsingDiagnostic(
                    modifierStart,
                    modifierStart + "private".length,
                    "expected no modifier before setter function, found 'private'",
                    hints = listOf(ParsingDiagnosticHint(
                        setterStart + "set".length,
                        setterStart + "set".length + 3,
                        "before setter function",
                    )),
                ),
            ),
            accessorSyntaxOnly = true,
        )

        val semicolonSeparated = "prop value: Int64 { get() { 1 }; set(value) {} }"
        assertBoth(
            "specific interface I { $semicolonSeparated }",
            SPECIFIC,
            emptyList(),
            accessorSyntaxOnly = true,
        )
    }

    /** 错误 accessor 前缀的恢复不能虚构 getter/setter；只有真实 accessor 才让 HasDefault 成立。 */
    @Test
    fun specificInterfaceDoesNotTreatRecoveredNonAccessorAsPropertyImplementation() {
        val property = "prop value: Int64 { private; }"
        val text = "specific interface I {\n$property\n}"
        assertBoth(text, SPECIFIC, listOf(range(
            text,
            property,
            "the member value must have body in 'specific' I",
        )), rulesOnly = true)

        for (prefix in listOf("private", "open()", "@Deprecated")) {
            val prefixedProperty = "prop value: Int64 { $prefix }"
            val prefixedText = "specific interface I { $prefixedProperty }"
            val expectedToken = when (prefix) {
                "@Deprecated" -> "}"
                else -> prefix.substringBefore('(')
            }
            val expectedMessage = "expected 'get' or 'set' in prop body, found '$expectedToken'"
            val prefixStart = prefixedText.lastIndexOf(prefix)
            val syntaxRange = if (expectedToken == "}") prefixedText.indexOf('}', prefixStart) else prefixStart
            assertBoth(
                prefixedText,
                SPECIFIC,
                listOf(ParsingDiagnostic(
                    syntaxRange,
                    syntaxRange + expectedToken.length,
                    expectedMessage,
                    hints = listOf(hintTokenPrefix(prefixedText, "value", "prop".length)),
                )),
                accessorSyntaxOnly = true,
            )
            assertBoth(
                prefixedText,
                SPECIFIC,
                listOf(ParsingDiagnostic(
                    startOffset = prefixedText.indexOf(prefixedProperty),
                    endOffset = prefixedText.length,
                    message = "the member value must have body in 'specific' I",
                )),
                rulesOnly = true,
            )
        }

        val recoveredProperty = "prop value: Int64 {\n    open()\n    get() { 1 }\n}"
        val followingMember = "func missing(): Unit"
        val recoveryText = "specific interface I {\n$recoveredProperty\n$followingMember\n}"
        assertBoth(
            recoveryText,
            SPECIFIC,
            listOf(
                range(
                    recoveryText,
                    "open",
                    "expected 'get' or 'set' in prop body, found 'open'",
                    hints = listOf(hintTokenPrefix(recoveryText, "value", "prop".length)),
                ),
                range(recoveryText, followingMember,
                    "the member missing must have body in 'specific' I"),
            ),
        )

        val openPrefix = "prop value: Int64 {\n    open(\n    get() { 1 }\n    )\n}"
        val openRecoveryText = "specific interface I {\n$openPrefix\n$followingMember\n}"
        assertBoth(
            openRecoveryText,
            SPECIFIC,
            listOf(
                range(
                    openRecoveryText,
                    "open",
                    "expected 'get' or 'set' in prop body, found 'open'",
                    hints = listOf(hintTokenPrefix(openRecoveryText, "value", "prop".length)),
                ),
                range(openRecoveryText, openPrefix,
                    "the member value must have body in 'specific' I"),
                range(openRecoveryText, followingMember,
                    "the member missing must have body in 'specific' I"),
            ),
        )

        val nestedAccessorProperty = "prop value: Int64 { bogus(\n get() { 1 }\n ) }"
        val nestedAccessorText = "specific interface I { $nestedAccessorProperty }"
        assertBoth(
            nestedAccessorText,
            SPECIFIC,
            listOf(
                range(
                    nestedAccessorText,
                    "bogus",
                    "expected 'get' or 'set' in prop body, found 'bogus'",
                    hints = listOf(hintTokenPrefix(nestedAccessorText, "value", "prop".length)),
                ),
                range(nestedAccessorText, nestedAccessorProperty,
                    "the member value must have body in 'specific' I").copy(endOffset = nestedAccessorText.length),
                ParsingDiagnostic(nestedAccessorText.length, nestedAccessorText.length, "Expecting '}'"),
            ),
        )

        val duplicateModifierProperty = "prop value: Int64 { private private get() { 1 } }"
        val duplicateModifierText = "specific interface I { $duplicateModifierProperty }"
        val firstPrivate = duplicateModifierText.indexOf("private")
        val secondPrivate = duplicateModifierText.indexOf("private", firstPrivate + 1)
        val getter = duplicateModifierText.indexOf("get()")
        assertBoth(
            duplicateModifierText,
            SPECIFIC,
            listOf(
                ParsingDiagnostic(
                    secondPrivate,
                    secondPrivate + "private".length,
                    "duplicated modifier: 'private'",
                    hints = listOf(ParsingDiagnosticHint(firstPrivate, firstPrivate + "private".length,
                        "previous one is here")),
                ),
                ParsingDiagnostic(
                    firstPrivate,
                    firstPrivate + "private".length,
                    "expected no modifier before getter function, found 'private'",
                    hints = listOf(hintAt(duplicateModifierText, getter + "get".length, 3,
                        "before getter function")),
                ),
            ),
            accessorSyntaxOnly = true,
        )

        val duplicateAccessorProperty = "prop value: Int64 { get() { 1 } get() { 2 } }"
        val duplicateAccessorText = "specific interface I { $duplicateAccessorProperty }"
        val firstGetter = duplicateAccessorText.indexOf("get()")
        val secondGetter = duplicateAccessorText.indexOf("get()", firstGetter + 1)
        val previousRangeStart = firstGetter + "get".length
        val previousRangeEnd = duplicateAccessorText.indexOf('}', firstGetter) + 1
        val propertyNameStart = duplicateAccessorText.indexOf("value")
        assertBoth(
            duplicateAccessorText,
            SPECIFIC,
            listOf(
                ParsingDiagnostic(
                    secondGetter,
                    secondGetter + "get".length,
                    "duplicated 'get' in prop",
                    hints = listOf(
                        ParsingDiagnosticHint(previousRangeStart, previousRangeEnd, "previous one"),
                        ParsingDiagnosticHint(propertyNameStart, propertyNameStart + "prop".length),
                    ),
                ),
            ),
            accessorSyntaxOnly = true,
        )

        val malformedAnnotation = "@Deprecated"
        val malformedAccessorPrefix = "$malformedAnnotation missing()\n    get() { 1 }"
        val malformedProperty = "prop value: Int64 {\n    $malformedAccessorPrefix\n}"
        val malformedText = "specific interface I {\n$malformedProperty\n$followingMember\n}"
        assertBoth(
            malformedText,
            SPECIFIC,
            listOf(
                range(malformedText, "missing",
                    "expected 'get' or 'set' in prop body, found 'missing'",
                    hints = listOf(hintTokenPrefix(malformedText, "value", "prop".length))),
                range(malformedText, followingMember,
                    "the member missing must have body in 'specific' I"),
            ),
        )
    }

    /**
     * finalizer 按 FUNC_DECL 的真实 body 判断 HasDefault；缺体负例仍报告 specific 缺实现。
     * 接口作用域没有 finalizer 修饰符规则，源码 common 不发布有效角色，也不产生外层 CJMP mismatch。
     * legacy-scope-expectations/cjc-results.json 的 interface-finalizer 只有原始作用域错误。
     */
    @Test
    fun specificInterfaceDoesNotReportBodyBearingFinalizerAsMissingImplementation() {
        val finalizer = "~init() {}"
        val text = "specific interface I {\n$finalizer\n}"
        assertBoth(text, SPECIFIC, emptyList(), rulesOnly = true)

        val finalizerWithoutBody = "~init()"
        val missingBodyText = "specific interface I {\n$finalizerWithoutBody\n}"
        assertBoth(missingBodyText, SPECIFIC, listOf(range(
            missingBodyText,
            finalizerWithoutBody,
            "the member ~init must have body in 'specific' I",
        )), rulesOnly = true)

        val conflictingFinalizer = "common ~init() {}"
        val conflictText = "specific interface I {\n$conflictingFinalizer\n}"
        assertBoth(conflictText, MIXED_WITH_COMMON_NESTING_DISABLED, emptyList(), rulesOnly = true)
    }

    /** 次构造器是官方 FuncDecl；接口作用域报错不能额外生成 specific 缺函数体诊断。 */
    @Test
    fun specificInterfaceDoesNotTreatBodyBearingSecondaryConstructorAsMissingImplementation() {
        val constructor = "init() {}"
        val text = "specific interface I { $constructor }"
        assertBoth(
            text,
            SPECIFIC,
            listOf(range(text, "init", "Invalid declaration in scope")),
        )
    }

    /** 外层 HasDefault 只检查直接成员；遇到成员角色冲突时官方流程不再追加该规则诊断。 */
    @Test
    fun specificInterfaceDoesNotRecurseIntoNestedMembersOrCheckAfterRoleConflict() {
        val nested = "interface Nested { func child(): Unit }"
        val nestedText = "specific interface I {\n$nested\n}"
        assertBoth(nestedText, SPECIFIC, listOf(range(
            nestedText,
            nested,
            "the member Nested must have body in 'specific' I",
        )), rulesOnly = true)

        val conflictingMember = "common func missing(): Unit"
        val conflictingText = "specific interface I {\n$conflictingMember\n}"
        assertBoth(conflictingText, MIXED_WITH_COMMON_NESTING_DISABLED, listOf(range(
            conflictingText,
            conflictingMember,
            "function missing is common, but interface is not common",
        )), rulesOnly = true)
    }

    /** HasDefault 对所有非函数/属性/变量种类返回 false，不能把名义声明有 body 当成实现。 */
    @Test
    fun specificInterfaceDoesNotTreatOtherDeclarationBodiesAsDefaults() {
        for (member in listOf("class Nested {}", "struct Nested {}", "interface Nested {}", "enum Nested { A }")) {
            val text = "specific interface I {\n$member\n}"
            assertBoth(text, SPECIFIC, listOf(range(text, member,
                "the member Nested must have body in 'specific' I")), rulesOnly = true)
        }
    }

    /** common 与 specific 谓词独立比较；alpha.20 的 common-in-specific 规则单独门控。 */
    @Test
    fun commonAndSpecificMembersFollowTheirIndependentOuterRoleRules() {
        for ((memberRole, context) in listOf("common" to COMMON, "specific" to SPECIFIC)) {
            for (outerKind in listOf("class", "struct", "interface", "enum")) {
                val member = "$memberRole func member(): Unit {}"
                val enumConstructor = if (outerKind == "enum") "A\n" else ""
                val text = "$outerKind Owner {\n$enumConstructor$member\n}"
                assertBoth(text, context, listOf(range(text, member,
                    "function member is $memberRole, but $outerKind is not $memberRole")))
            }
        }
        for (outerKind in listOf("class", "struct", "interface", "enum")) {
            val member = "common func member(): Unit {}"
            val enumEntry = if (outerKind == "enum") "A\n" else ""
            val text = "specific $outerKind Owner {\n$enumEntry$member\n}"
            assertBoth(text, MIXED_WITH_COMMON_NESTING_DISABLED, listOf(range(text, member,
                "function member is common, but $outerKind is not common")))
            assertBoth(text, MIXED, emptyList())
        }
        val extendMember = "common func member(): Unit {}"
        val specificExtend = "specific extend Int64 {\n$extendMember\n}"
        assertBoth(specificExtend, MIXED_WITH_COMMON_NESTING_DISABLED, listOf(range(
            specificExtend,
            extendMember,
            "function member is common, but extend is not common",
        )))
        assertBoth(specificExtend, MIXED, emptyList())

        val specificMember = "specific func member(): Unit {}"
        val commonOuter = "common interface Owner {\n$specificMember\n}"
        assertBoth(commonOuter, MIXED, listOf(range(commonOuter, specificMember,
            "function member is specific, but interface is not specific")))

        val unmarkedOuter = "class Owner { common func member(): Unit {} }"
        assertBoth(unmarkedOuter, MIXED, listOf(range(unmarkedOuter, "common func member(): Unit {}",
            "function member is common, but class is not common")))
        for ((role, context) in listOf("common" to COMMON, "specific" to SPECIFIC)) {
            assertBoth("$role interface Owner { $role func member(): Unit {} }", context, emptyList())
            assertBoth("$role func topLevel(): Unit {}", context, emptyList())
        }
    }

    /** STATIC + CONSTRUCTOR 才触发 static-init 规则，静态函数不得误报。 */
    @Test
    fun staticInitializerReportsItsDeclarationAndParentStillChecksItsDirectMember() {
        val member = "common static init() {}"
        val text = "class Owner {\n$member\nstatic func ordinary(): Unit {}\n}"
        // v1.1.3 cjmp-static-init.cj：声明自身先报告 static-init，同范围的外层 mismatch 不进入 final。
        assertBoth(text, COMMON, listOf(
            range(text, member, "static init can not be 'common'"),
        ))
        val primary = "common static Owner() {}"
        val primaryText = "common class Owner { $primary }"
        assertBoth(primaryText, COMMON, listOf(range(primaryText, primary, "static init can not be 'common'")))
        val staticPrimaryParameter = "common static C(specific let x: Int64) {}"
        val mixedText = "class C { $staticPrimaryParameter }"
        assertBoth(mixedText, MIXED, listOf(
            range(mixedText, staticPrimaryParameter, "static init can not be 'common'"),
        ), rulesOnly = true)
        for ((role, context) in listOf("common" to COMMON, "specific" to SPECIFIC)) {
            assertBoth("$role interface Owner { $role static func member(): Unit {} }", context, emptyList())
            assertBoth("class Owner { static init() {} }", context, emptyList())
        }
    }

    /** primary constructor 的 let/var 参数按其自身外层声明比较 CJMP，不能借用所属 class 的角色。 */
    @Test
    fun primaryConstructorMemberParametersReportTheirOwnOuterMismatch() {
        for ((role, context) in listOf("common" to COMMON, "specific" to SPECIFIC)) {
            val parameter = "$role let first: Int64"
            val text = "class C {\n    C($parameter) {}\n}"
            assertBoth(text, context, listOf(
                range(text, parameter, "parameter first is $role, but primary constructor is not $role"),
            ), rulesOnly = true)

            val commented = "class C { C($parameter /* trailing trivia */) {} }"
            assertBoth(commented, context, listOf(
                range(commented, parameter, "parameter first is $role, but primary constructor is not $role"),
            ), rulesOnly = true)
        }

        val broken = "common let first"
        val brokenText = "class C { C($broken) {} }"
        assertBoth(brokenText, COMMON, listOf(
            range(brokenText, broken, "parameter first is common, but primary constructor is not common"),
        ), rulesOnly = true)

        val brokenWithComment = "$broken /* trailing trivia */"
        val brokenCommentText = "class C { C($brokenWithComment) {} }"
        val brokenStart = brokenCommentText.indexOf(brokenWithComment)
        val brokenEnd = brokenCommentText.indexOf(')', brokenStart)
        assertBoth(brokenCommentText, COMMON, listOf(
            ParsingDiagnostic(brokenStart, brokenEnd,
                "parameter first is common, but primary constructor is not common"),
        ), rulesOnly = true)
    }

    @Test
    fun primaryConstructorParameterMatchingUsesItsIndependentVersionGate() {
        val parameter = "specific let first: Int64"
        val text = "class C { C($parameter) {} }"
        val expected = range(text, parameter,
            "parameter first is specific, but primary constructor is not specific")
        assertBoth(text, SPECIFIC, listOf(expected), rulesOnly = true)

        assertBoth(text, SPECIFIC.copy(parameterRoleMatching = false), emptyList(), rulesOnly = true)
        assertBoth(text, MIXED, emptyList(), rulesOnly = true)

        val distinctOwners = "specific class C { common C(specific let first: Int64) {} }"
        val constructorParameter = "specific let first: Int64"
        // Parameter role is compared with the primary constructor (common), not its specific class.
        assertBoth(
            distinctOwners,
            MIXED.copy(parameterRoleMatching = true),
            listOf(
                range(distinctOwners, "common", "modifier 'common' is not allowed on this declaration"),
                range(distinctOwners, constructorParameter,
                    "parameter first is specific, but primary constructor is not specific"),
            ),
            rulesOnly = true,
        )

        val multipleLists = "class C { C(specific let first: Int64)() {} }"
        assertBoth(multipleLists, SPECIFIC, emptyList(), rulesOnly = true)
    }

    /** alpha.65 的 common/platform 泛型禁令归 parser；alpha.66 删除该拒绝并改为 @Frozen 语义诊断。 */
    @Test
    fun cjmpGenericDeclarationsFollowTheAlphaSixtySixParserBoundary() {
        val oldVersion = PLATFORM.copy(genericDeclarationsAvailable = false)
        val classText = "platform class Box<T> { Box() {} }"
        val genericMessage = "generic declaration can not be 'platform'"
        assertBoth(classText, oldVersion, listOf(range(classText, classText, genericMessage)))
        assertBoth(classText, PLATFORM, emptyList())

        val functionText = "platform func identity<T>(value: T): T { value }"
        assertBoth(functionText, oldVersion, listOf(range(functionText, functionText, genericMessage)))
        assertBoth(functionText, PLATFORM, emptyList())

        val commonText = "common class Box<T> { Box() {} }"
        assertBoth(commonText, oldVersion.copy(compileCommon = true, compileSpecific = false), listOf(
            range(commonText, commonText, "generic declaration can not be 'common'"),
        ))

        val aliasText = "platform type Alias<T> = T"
        assertBoth(aliasText, oldVersion, listOf(
            range(aliasText, "platform", "modifier 'platform' is not allowed on this declaration"),
            range(aliasText, aliasText, genericMessage),
        ))

        val quotedText = "func quoted(): Unit { let q = quote(platform class Box<T> {}) }"
        assertBoth(quotedText, oldVersion, emptyList())

        val unavailableSpecificText = "specific class Box<T> {}"
        assertBoth(unavailableSpecificText, oldVersion, listOf(
            range(unavailableSpecificText, "specific", "modifier 'specific' is unavailable in this language version"),
        ))
    }

    /** alpha.65 仅拒绝 platform 默认参数；common 默认仍可声明，alpha.66 后双方冲突交给匹配阶段。 */
    @Test
    fun platformParameterDefaultsFollowTheAlphaSixtySixParserBoundary() {
        val oldVersion = PLATFORM.copy(
            genericDeclarationsAvailable = false,
            parameterDefaultValueRulesAvailable = false,
        )
        val message = "'platform' function parameter can not have default value"
        val function = "platform func value(x!: Int64 = 1): Int64 { x }"
        assertBoth(function, oldVersion, listOf(range(function, "x!: Int64 = 1", message)))
        assertBoth(function, PLATFORM, emptyList())
        assertBoth("specific func value(x!: Int64 = 1): Int64 { x }", SPECIFIC, emptyList())
        assertBoth(function, oldVersion.copy(enabled = false), emptyList())

        val genericFunction = "platform func identity<T>(x!: Int64 = 1): Int64 { x }"
        val genericMessage = "generic declaration can not be 'platform'"
        assertBoth(genericFunction, oldVersion, listOf(range(genericFunction, genericFunction, genericMessage)))

        val staticFunction = "platform class C { static platform func value(x!: Int64 = 1): Int64 { x } }"
        assertBoth(staticFunction, oldVersion, listOf(range(staticFunction, "x!: Int64 = 1", message)))

        val primary = "platform class C { platform C(x!: Int64 = 1) {} }"
        val primaryMessage = "'platform' primary constructor parameter can not have default value"
        assertBoth(primary, oldVersion, listOf(range(primary, "x!: Int64 = 1", primaryMessage)))

        val secondary = "platform class C { platform init(x!: Int64 = 1) {} }"
        val secondaryMessage = "'platform' constructor parameter can not have default value"
        assertBoth(secondary, oldVersion, listOf(range(secondary, "x!: Int64 = 1", secondaryMessage)))

        val secondaryWithMultipleLists = "platform class C { platform init(x!: Int64 = 1)() {} }"
        assertBoth(secondaryWithMultipleLists, oldVersion, emptyList())

        val primaryWithMultipleLists = "platform class C { C(x!: Int64 = 1)() {} }"
        assertBoth(primaryWithMultipleLists, oldVersion, emptyList())

        val staticSecondary = "platform class C { static platform init(x!: Int64 = 1) {} }"
        assertBoth(staticSecondary, oldVersion, listOf(
            range(staticSecondary, "static platform init(x!: Int64 = 1) {}", "static init can not be 'platform'"),
        ))

        val common = "common func value(x!: Int64 = 1): Int64 { x }"
        assertBoth(common, oldVersion.copy(compileCommon = true, compileSpecific = false), emptyList())
    }

    /**
     * 有效泛型 CJMP 声明带内建 @Frozen 时短路该声明的 CJMP 检查。
     * 嵌套 class 的已知作用域缺表，COMMON 属性清空；nested 官方探针只有 class 作用域错误。
     * 因此既有 CJMP 规则过滤后的期望为空，不能根据源码 common 追加外层 mismatch。
     */
    @Test
    fun genericFrozenDeclarationShortCircuitsItsCjmpRules() {
        assertBoth("@Frozen common class C<T> {}", COMMON, emptyList(), rulesOnly = true)
        assertBoth("@Frozen specific interface I<T> { func missing(): Unit }", SPECIFIC,
            emptyList(), rulesOnly = true)
        val nestedRole = "common class Nested<T> {}"
        val nested = "class Outer { @Frozen $nestedRole }"
        assertBoth(nested, COMMON, emptyList(), rulesOnly = true)
    }

    /** common 类和结构体必须包含直接解析出的构造器，隐式构造器与嵌套成员都不计数。 */
    @Test
    fun commonConstructorRequirementUsesTheIdentifierAndDirectConstructorFacts() {
        for (kind in listOf("class", "struct")) {
            val message = "at least one constructor is required in common $kind 'Owner'"
            for (body in listOf("", "func member(): Unit {}", "class Nested { Nested() {} }")) {
                val text = "common $kind Owner {\n$body\n}"
                assertBoth(text, COMMON, listOf(range(text, "Owner", message)), rulesOnly = true)
            }
            for (constructor in listOf("Owner() {}", "init() {}", "common init()")) {
                assertBoth("common $kind Owner {\n$constructor\n}", COMMON, emptyList())
            }
            assertBoth("specific $kind Owner {}", SPECIFIC, emptyList())
        }
        val abstract = "common abstract class Owner {}"
        assertBoth(abstract, COMMON, listOf(range(abstract, "Owner",
            "at least one constructor is required in common class 'Owner'")))
        assertBoth("common interface Owner {}", COMMON, emptyList())
        assertBoth("common enum Owner { A }", COMMON, emptyList())
    }

    /** 函数锚整个声明，属性锚最后一个源码 token 的末尾，不能包含尾随空白或注释。 */
    @Test
    fun explicitAbstractFunctionAndPropertyUseTheirNativeAnchors() {
        for (outer in listOf("abstract class Owner", "interface Owner")) {
            val function = "abstract func missing(): Unit"
            val text = "$outer {\n$function // trailing comment\n}"
            assertBoth(text, CjmpParsingContext.SyntaxOnly,
                listOf(range(text, function, abstractMessage("function"))))
            val property = "abstract prop missing: Int64"
            val propertyText = "$outer {\n$property /* trailing comment */\n}"
            assertBoth(propertyText, CjmpParsingContext.SyntaxOnly,
                listOf(endPoint(propertyText, property, abstractMessage("property"))))
        }
    }

    /** 非 class/interface 的声明作用域不能从最近的祖先错误继承 abstract 规则。 */
    @Test
    fun explicitAbstractChecksOnlyClassAndInterfaceDirectMembers() {
        for (text in listOf(
            "abstract func topLevel(): Unit",
            "struct Owner { abstract func member(): Unit }",
            "enum Owner { A\nabstract func member(): Unit }",
            "extend Int64 { abstract func member(): Unit }",
            "class Owner { func host(): Unit { abstract func local(): Unit } }",
        )) {
            assertBoth(text, CjmpParsingContext.SyntaxOnly, emptyList(), rulesOnly = true)
        }
        val member = "abstract func nested(): Unit"
        val text = "class Outer { interface Inner { $member } }"
        assertBoth(text, CjmpParsingContext.SyntaxOnly,
            listOf(range(text, member, abstractMessage("function"))), rulesOnly = true)
    }

    /** 外层 abstract+CJMP、成员自身 COMMON 以及真正的 Java mirror 语法身份分别豁免。 */
    @Test
    fun explicitAbstractExemptionsUseBothOuterFlagsAndBuiltinAnnotationIdentity() {
        for ((role, context) in listOf("common" to COMMON, "specific" to SPECIFIC)) {
            val constructor = if (role == "common") "init() {}\n" else ""
            val text = "$role abstract class Owner {\n${constructor}abstract func member(): Unit\nabstract prop value: Int64\n}"
            assertBoth(text, context, emptyList())
        }
        assertBoth("common interface Owner { common abstract func member(): Unit\ncommon abstract prop value: Int64 }",
            COMMON, emptyList())
        // 官方 JavaImpl 派生 JAVA_MIRROR_SUBTYPE；没有独立的 JavaMirrorSubtype 源码注解。
        for (annotation in listOf("@JavaMirror[\"java.lang.Object\"]", "@JavaImpl[\"java.lang.Object\"]")) {
            assertBoth("$annotation\nabstract class Owner { abstract func member(): Unit\nabstract prop value: Int64 }",
                CjmpParsingContext.SyntaxOnly, emptyList())
        }
        for (annotation in listOf("@interop.JavaMirror[\"java.lang.Object\"]", "@!JavaMirror[\"java.lang.Object\"]")) {
            val member = "abstract func member(): Unit"
            val text = "$annotation\nabstract class Owner { $member }"
            assertBoth(text, CjmpParsingContext.SyntaxOnly, listOf(range(text, member, abstractMessage("function"))),
                rulesOnly = true)
        }
    }

    /** 前四项要求有效编译角色；explicitly abstract 独立于模式，仅受版本门约束。 */
    @Test
    fun declarationRulesRespectVersionAndCompilationMode() {
        for (text in listOf("common class Owner {}", "specific interface Owner { func missing(): Unit }")) {
            assertBoth(text, CjmpParsingContext.SyntaxOnly, emptyList())
            assertBoth(text, CjmpParsingContext.Compilation(true, true, false), emptyList())
        }
        val member = "abstract func missing(): Unit"
        val text = "abstract class Owner { $member }"
        assertBoth(text, CjmpParsingContext.Compilation(false, false, true),
            listOf(range(text, member, abstractMessage("function"))))
        assertBoth(text, CjmpParsingContext.Compilation(false, false, false), emptyList())
    }

    /** modifier 目标和冲突是共享 parser 的语法规则，PSI 与 LightTree 必须给出同一 token 范围。 */
    @Test
    fun cjmpModifierTargetsAreCheckedInTheirDeclarationScopes() {
        val invalidTargets = listOf(
            "common type Alias = Int64",
            "common main(): Unit {}",
            "class Owner { specific Owner() {} }",
            "func host(): Unit { common let value: Int64 = 1 }",
        )
        for (text in invalidTargets) {
            val modifier = if (text.contains("specific")) "specific" else "common"
            assertBoth(
                text,
                CjmpParsingContext.SyntaxOnly,
                listOf(range(text, modifier, "modifier '$modifier' is not allowed on this declaration")),
                rulesOnly = true,
            )
        }

        assertBoth("common class Owner {}", CjmpParsingContext.SyntaxOnly, emptyList(), rulesOnly = true)
        assertBoth("specific func platform(): Unit {}", CjmpParsingContext.SyntaxOnly, emptyList(), rulesOnly = true)
        val oldLanguage = CjmpParsingContext.Compilation(
            compileCommon = false,
            compileSpecific = false,
            enabled = false,
            modifierSyntaxAvailable = false,
        )
        val oldVersionDeclaration = "common class Owner {}"
        assertBoth(
            oldVersionDeclaration,
            oldLanguage,
            listOf(range(
                oldVersionDeclaration,
                "common",
                "modifier 'common' is unavailable in this language version",
            )),
            rulesOnly = true,
        )
        assertBoth(
            "class Owner { common private func member(): Unit {} }",
            CjmpParsingContext.SyntaxOnly,
            emptyList(),
            rulesOnly = true,
        )
    }

    @Test
    fun cjmpModifierRulesCoverEnumEntriesAndForeignFunctions() {
        val enumEntry = "common A"
        val enumText = "enum E { | $enumEntry }"
        assertBoth(
            enumText,
            CjmpParsingContext.SyntaxOnly,
            listOf(range(enumText, "common", "modifier 'common' is not allowed on this declaration")),
            rulesOnly = true,
        )
        val enumFileRoleText = "$enumText\nspecific class S {}"
        assertBoth(
            enumFileRoleText,
            CjmpParsingContext.Compilation(false, true, true),
            listOf(range(enumFileRoleText, "common", "modifier 'common' is not allowed on this declaration")),
        )

        val foreignModifier = "common"
        val foreignText = "foreign { $foreignModifier func f(): Unit }"
        assertBoth(
            foreignText,
            CjmpParsingContext.SyntaxOnly,
            listOf(modifierConflict(foreignText, foreignModifier, "foreign", "func", "function declaration")),
            rulesOnly = true,
        )
    }

    /** 冲突按官方 scope 表和源码遍历方向检查；extend 成员的 common private 留给 CFIR 语义检查。 */
    @Test
    fun cjmpModifierConflictsUseScopedDirectionalRules() {
        val nominal = "private common class Owner {}"
        assertBoth(
            nominal,
            CjmpParsingContext.SyntaxOnly,
            listOf(modifierConflict(nominal, "common", "private", "class", "class declaration")),
            rulesOnly = true,
        )

        val function = "private specific func platform(): Unit {}"
        assertBoth(
            function,
            CjmpParsingContext.SyntaxOnly,
            listOf(modifierConflict(function, "specific", "private", "func", "function declaration")),
            rulesOnly = true,
        )
        assertBoth("specific private func platform(): Unit {}", CjmpParsingContext.SyntaxOnly, emptyList(), rulesOnly = true)

        val constFunction = "class Owner { const common func value(): Int64 { 1 } }"
        assertBoth(
            constFunction,
            CjmpParsingContext.SyntaxOnly,
            listOf(modifierConflict(constFunction, "common", "const", "func", "function declaration")),
            rulesOnly = true,
        )

        assertBoth(
            "common extend Int64 { common private func value(): Int64 { 1 } }",
            CjmpParsingContext.SyntaxOnly,
            emptyList(),
            rulesOnly = true,
        )
        val invalidExtend = "common private extend Int64 {}"
        // 官方 extend-private 探针按 native token 顺序选择 PRIVATE，主范围为 7..14；此处比较完整诊断。
        assertBoth(
            invalidExtend,
            CjmpParsingContext.SyntaxOnly,
            listOf(range(
                invalidExtend,
                "private",
                CangJieParsingBundle.message("parsing.error.expected.no.modifier.before", "extend declaration", "private"),
                hints = listOf(ParsingDiagnosticHint(
                    invalidExtend.indexOf("extend"), invalidExtend.indexOf("extend") + "extend".length,
                    CangJieParsingBundle.message("parsing.error.hint.before", "extend declaration"),
                )),
            )),
        )
    }

    /** CJMP modifier conflicts follow the official declaration-kind and container-specific parser tables. */
    @Test
    fun cjmpModifierConflictsCoverTheOfficialScopeMatrix() {
        fun assertConflict(text: String, conflicting: String, earlier: String, keyword: String, declarationKind: String) {
            assertBoth(
                text,
                CjmpParsingContext.SyntaxOnly,
                listOf(modifierConflict(text, conflicting, earlier, keyword, declarationKind)),
                modifierSyntaxOnly = true,
            )
        }

        fun assertNoConflict(text: String) {
            assertBoth(text, CjmpParsingContext.SyntaxOnly, emptyList(), modifierSyntaxOnly = true)
        }

        fun assertConflicts(text: String, conflicts: List<Pair<String, String>>, keyword: String, declarationKind: String) {
            assertBoth(
                text,
                CjmpParsingContext.SyntaxOnly,
                conflicts.map { (modifier, earlier) ->
                    modifierConflict(text, modifier, earlier, keyword, declarationKind)
                },
                modifierSyntaxOnly = true,
            )
        }

        assertConflict("private common class TopClass {}", "common", "private", "class", "class declaration")
        assertConflict("specific private interface TopInterface {}", "private", "specific", "interface", "interface declaration")
        assertConflict("private specific struct TopStruct {}", "specific", "private", "struct", "struct declaration")
        assertConflict("common private enum TopEnum { A }", "private", "common", "enum", "enum declaration")

        assertConflict("const common func topCommonConst(): Unit {}", "common", "const", "func", "function declaration")
        assertConflict("specific const func topSpecificConst(): Unit {}", "const", "specific", "func", "function declaration")
        val topFunctionModifierTriples = listOf(
            "const foreign common func topConstForeignCommon(): Unit" to
                listOf("foreign" to "const", "common" to "const"),
            "foreign const common func topForeignConstCommon(): Unit" to
                listOf("const" to "foreign", "common" to "const"),
        )
        // COMMON 数组先 CONST 再 FOREIGN；反序输入改变前一条冲突，但不改变 COMMON 范围的首报。
        for ((text, conflicts) in topFunctionModifierTriples) {
            assertConflicts(
                text,
                conflicts,
                "func", "function declaration",
            )
        }
        assertConflict("foreign common func topCommonForeign(): Unit", "common", "foreign", "func", "function declaration")
        assertNoConflict("specific foreign func topSpecificForeign(): Unit")
        assertNoConflict("foreign specific func topForeignSpecific(): Unit")
        assertConflict("const common let topConstCommon: Int64 = 0", "common", "const", "let", "variable declaration")
        assertConflict("common const let topCommonConst: Int64 = 0", "const", "common", "let", "variable declaration")
        assertConflict("const specific let topConstSpecific: Int64 = 0", "specific", "const", "let", "variable declaration")
        assertConflict("specific const let topSpecificConst: Int64 = 0", "const", "specific", "let", "variable declaration")
        assertConflict("specific foreign let topSpecificForeignValue: Int64 = 0", "foreign", "specific", "let", "variable declaration")
        assertConflict("foreign common let topCommonForeignValue: Int64 = 0", "common", "foreign", "let", "variable declaration")
        val multipleConflicts = "const common specific func multipleConflicts(): Unit {}"
        // SPECIFIC 数组同样先 CONST 再 COMMON；同一 SPECIFIC 原生范围仅发布 specific/const。
        assertBoth(
            multipleConflicts,
            CjmpParsingContext.SyntaxOnly,
            listOf(
                modifierConflict(multipleConflicts, "common", "const", "func", "function declaration"),
                modifierConflict(multipleConflicts, "specific", "const", "func", "function declaration"),
            ),
            modifierSyntaxOnly = true,
        )

        val memberPrivateCases = listOf(
            "class Owner { common private func f(): Unit {} }",
            "interface Owner { specific private func f(): Unit }",
            "struct Owner { common private func f(): Unit {} }",
            "enum Owner { A\n specific private func f(): Unit {} }",
            "specific extend Int64 { specific private func f(): Unit {} }",
            "class Owner { common private var value: Int64 = 0 }",
            "struct Owner { specific private var value: Int64 = 0 }",
            "class Owner { common private prop value: Int64 { get() { 0 } } }",
            "interface Owner { specific private prop value: Int64 { get() { 0 } } }",
            "struct Owner { common private prop value: Int64 { get() { 0 } } }",
            "enum Owner { A\n specific private prop value: Int64 { get() { 0 } } }",
            "specific extend Int64 { specific private prop value: Int64 { get() { 0 } } }",
            "class Owner { common private init() {} }",
        )
        memberPrivateCases.forEach(::assertNoConflict)

        assertConflict("class Owner { const common func f(): Unit {} }", "common", "const", "func", "function declaration")
        assertConflict("interface Owner { specific const func f(): Unit }", "const", "specific", "func", "function declaration")
        assertConflict("struct Owner { common const func f(): Unit {} }", "const", "common", "func", "function declaration")
        assertConflict("enum Owner { A\n const specific func f(): Unit {} }", "specific", "const", "func", "function declaration")
        assertConflict("class Owner { specific common var value: Int64 = 0 }", "common", "specific", "var", "variable declaration")
        assertConflict("struct Owner { common specific var value: Int64 = 0 }", "specific", "common", "var", "variable declaration")
        assertConflict("class Owner { const common let commonValue: Int64 = 0 }", "common", "const", "let", "variable declaration")
        assertConflict("class Owner { const specific let specificValue: Int64 = 0 }", "specific", "const", "let", "variable declaration")
        assertConflict("struct Owner { common const let commonValue: Int64 = 0 }", "const", "common", "let", "variable declaration")
        assertConflict("struct Owner { specific const let specificValue: Int64 = 0 }", "const", "specific", "let", "variable declaration")

        assertConflict(
            "class Owner { specific common prop value: Int64 { get() { 0 } } }",
            "common",
            "specific",
            "prop", "property declaration",
        )
        val extendProperty = "specific extend Int64 { common specific prop value: Int64 { get() { 0 } } }"
        assertBoth(
            extendProperty,
            CjmpParsingContext.SyntaxOnly,
            listOf(modifierConflict(
                extendProperty, "specific", "common", "prop", "property declaration",
                fromIndex = extendProperty.indexOf('{') + 1,
            )),
            modifierSyntaxOnly = true,
        )

        assertConflict("class Owner { specific common init() {} }", "common", "specific", "init", "function declaration")
        assertBoth(
            "class Owner { specific Owner() {} }",
            CjmpParsingContext.SyntaxOnly,
            listOf(range(
                "class Owner { specific Owner() {} }",
                "specific",
                "modifier 'specific' is not allowed on this declaration",
            )),
            modifierSyntaxOnly = true,
        )
    }

    @Test
    fun primaryConstructorRolesFollowTheirOwnAlpha66VersionGate() {
        val legacyPrimaryRole = CjmpParsingContext.Compilation(
            compileCommon = true,
            compileSpecific = true,
            enabled = true,
            mixedCommonSpecificDeclarations = true,
            modifierSyntaxAvailable = false,
            platformModifierSyntaxAvailable = true,
            primaryConstructorRoleModifiersAvailable = true,
        )
        val accepted = "common class Owner { common Owner() {} }"
        assertBoth(accepted, legacyPrimaryRole, emptyList(), modifierSyntaxOnly = true)

        val conflicting = "common class Owner { common platform Owner() {} }"
        assertBoth(
            conflicting,
            legacyPrimaryRole,
            listOf(modifierConflict(
                conflicting, "platform", "common", "Owner", "constructor",
                fromIndex = conflicting.indexOf('{') + 1,
            )),
            modifierSyntaxOnly = true,
        )

        val afterAlpha66 = legacyPrimaryRole.copy(primaryConstructorRoleModifiersAvailable = false)
        val rejected = "common class Owner { platform Owner() {} }"
        assertBoth(
            rejected,
            afterAlpha66,
            listOf(range(rejected, "platform", "modifier 'platform' is not allowed on this declaration")),
            modifierSyntaxOnly = true,
        )
    }

    @Test
    fun cjmpRepeatedModifiersAreReportedByTheParser() {
        val text = "common common class Owner {}"
        val firstCommon = text.indexOf("common")
        val secondCommon = text.lastIndexOf("common")
        // 官方 v1.1.3 的 parse_duplicate_modifier 在第二次 token 插入时报告，并提示首次出现位置。
        assertBoth(
            text,
            CjmpParsingContext.SyntaxOnly,
            listOf(ParsingDiagnostic(
                secondCommon,
                secondCommon + "common".length,
                "duplicated modifier: 'common'",
                hints = listOf(ParsingDiagnosticHint(
                    firstCommon, firstCommon + "common".length, "previous one is here",
                )),
            )),
        )
    }

    /** quote 中的声明是宏 token 输入，不是当前文件的 CJMP 声明或 direct member。 */
    @Test
    fun quotedTokensCannotCreateDeclarationRuleFactsOrFileRoleFailures() {
        val text = "func tokens(): Unit { let q = quote(common class Owner {} " +
            "specific interface I { func missing(): Unit } " +
            "class Plain { common static init() {} abstract func f(): Unit }) }"
        for (context in listOf(COMMON, SPECIFIC, CjmpParsingContext.Compilation(false, false, true))) {
            assertBoth(text, context, emptyList())
        }
    }

    /**
     * 普通声明角色失败后清空有效属性；secondary 在 body 前预置的 COMMON 是独立例外。
     * 官方 ParseDecl.cpp:1014–1025 保留预置 COMMON，ParseCJMPDecl.cpp:282–284 仍按有效属性比较外层。
     * secondary 官方探针在 rejected 模式报文件错误和 mismatch；NONE 未启用 CJMP 规则，仅报文件错误。
     */
    @Test
    fun fileRoleFailureClearsAttributesExceptSecondaryPreassignedCommon() {
        for ((role, message) in listOf(
            "common" to "common declaration must be defined in common package part",
            "specific" to "specific declaration must be defined in specific package part",
        )) {
            val rejected = if (role == "common") CjmpParsingContext.Compilation(false, true, true)
                else CjmpParsingContext.Compilation(true, false, true)
            val sources = listOf(
                "$role class Owner {}",
                "$role interface Owner { func missing(): Unit }",
                "class Owner { $role static init() {} }",
            )
            for (text in sources) {
                val fileError = ParsingDiagnostic(0, 0, message, ParsingDiagnostic.Origin.ORIGINAL_FILE)
                val rejectedExpected = if (role == "common" && text == "class Owner { common static init() {} }") {
                    // 检查失败不发布 STATIC，但预置 COMMON 保留；期望完整构造器的外层 mismatch。
                    listOf(fileError, ParsingDiagnostic(14, 37, "constructor init is common, but class is not common"))
                } else listOf(fileError)
                assertBoth(text, rejected, rejectedExpected)
                assertBoth(text, CjmpParsingContext.Compilation(false, false, true), listOf(fileError))
            }
        }
        val text = "specific interface First {}\ncommon class Rejected {}"
        assertBoth(text, CjmpParsingContext.Compilation(true, true, true), listOf(
            ParsingDiagnostic(0, 0, "'common' and 'specific' declarations can not be in the same file",
                ParsingDiagnostic.Origin.ORIGINAL_FILE),
        ))

        val invalidTargetAfterRoleFailure = "specific type Alias = Int64"
        assertBoth(invalidTargetAfterRoleFailure, CjmpParsingContext.Compilation(true, false, true), listOf(
            ParsingDiagnostic(0, 0, "specific declaration must be defined in specific package part",
                ParsingDiagnostic.Origin.ORIGINAL_FILE),
        ))
    }

    private fun assertBoth(
        text: String,
        context: CjmpParsingContext,
        expected: List<ParsingDiagnostic>,
        rulesOnly: Boolean = false,
        modifierSyntaxOnly: Boolean = false,
        accessorSyntaxOnly: Boolean = false,
    ) {
        val file = createPsiFile("cjmpDeclarationRules", text) as CjFile
        file.cjmpParsingContext = context
        val psiPublication = file.collectParsingDiagnosticPublication()
        val psiDiagnostics = psiPublication.diagnostics
        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        val lightResult = parseLightWithPsiInput(file, builder)
        lightDiagnostics += lightResult.diagnostics
        val sourceContext = checkNotNull(publishedParserSeed(file).sourceContext)
        assertCanonicalParsingDiagnostics(psiDiagnostics, sourceContext, text)
        assertCanonicalParsingDiagnostics(lightDiagnostics, sourceContext, text)
        assertPublishedParsingDiagnosticParity(sorted(psiDiagnostics), sorted(lightDiagnostics), sourceContext, text)
        assertLegacyParsingDiagnostics(sorted(expected), sorted(when {
            modifierSyntaxOnly -> psiDiagnostics.filter(::isModifierSyntaxDiagnostic)
            accessorSyntaxOnly -> psiDiagnostics.filter(::isPropertyAccessorSyntaxDiagnostic)
            rulesOnly -> psiDiagnostics.filter(::isRuleDiagnostic)
            else -> psiDiagnostics
        }),
            sourceContext, "PSI: $text")
        assertLegacyParsingDiagnostics(sorted(expected), sorted(when {
            modifierSyntaxOnly -> lightDiagnostics.filter(::isModifierSyntaxDiagnostic)
            accessorSyntaxOnly -> lightDiagnostics.filter(::isPropertyAccessorSyntaxDiagnostic)
            rulesOnly -> lightDiagnostics.filter(::isRuleDiagnostic)
            else -> lightDiagnostics
        }),
            sourceContext, "LightTree: $text")
    }

    /** 官方 ParserDiag.cpp:557–601 的两个 hint：先指向既有 modifier，再指向当前声明自己的关键字。 */
    private fun modifierConflict(
        text: String,
        conflicting: String,
        earlier: String,
        declarationKeyword: String,
        declarationKind: String,
        fromIndex: Int = 0,
    ): ParsingDiagnostic {
        val conflictingStart = text.indexOf(conflicting, fromIndex)
        val earlierStart = text.indexOf(earlier, fromIndex)
        val keywordStart = text.indexOf(declarationKeyword, fromIndex)
        require(conflictingStart >= 0 && earlierStart >= 0 && keywordStart >= 0)
        return ParsingDiagnostic(
            conflictingStart, conflictingStart + conflicting.length,
            CangJieParsingBundle.message("parsing.error.cjmp.conflicting.modifiers", conflicting, earlier),
            hints = listOf(
                ParsingDiagnosticHint(
                    earlierStart, earlierStart + earlier.length,
                    CangJieParsingBundle.message("parsing.error.hint.conflicted.modifier"),
                ),
                ParsingDiagnosticHint(
                    keywordStart, keywordStart + declarationKeyword.length,
                    CangJieParsingBundle.message("parsing.error.hint.on", declarationKind),
                ),
            ),
        )
    }

    private fun range(
        text: String,
        declaration: String,
        message: String,
        hints: List<ParsingDiagnosticHint> = emptyList(),
    ): ParsingDiagnostic {
        val start = text.indexOf(declaration)
        require(start >= 0)
        return ParsingDiagnostic(start, start + declaration.length, message, hints = hints)
    }

    private fun hintAfterToken(
        text: String,
        token: String,
        width: Int,
        label: String? = null,
    ): ParsingDiagnosticHint {
        val tokenStart = text.lastIndexOf(token)
        require(tokenStart >= 0)
        return hintAt(text, tokenStart + token.length, width, label)
    }

    private fun hintTokenPrefix(
        text: String,
        token: String,
        width: Int,
        label: String? = null,
    ): ParsingDiagnosticHint {
        val tokenStart = text.indexOf(token)
        require(tokenStart >= 0)
        return hintAt(text, tokenStart, width, label)
    }

    private fun hintAt(text: String, startOffset: Int, width: Int, label: String?): ParsingDiagnosticHint {
        require(startOffset in 0..text.length && width >= 0)
        return ParsingDiagnosticHint(startOffset, (startOffset + width).coerceAtMost(text.length), label)
    }

    private fun endPoint(text: String, declaration: String, message: String): ParsingDiagnostic {
        val end = range(text, declaration, message).endOffset
        return ParsingDiagnostic(end, end, message)
    }

    private fun abstractMessage(kind: String): String =
        "only common/specific or Native FFI mirror abstract classes can have explicitly abstract $kind"

    private fun isRuleDiagnostic(diagnostic: ParsingDiagnostic): Boolean = with(diagnostic.message) {
        startsWith("the member ") && contains(" must have body in 'specific' ") ||
            contains(" is common, but ") || contains(" is specific, but ") ||
            startsWith("static init can not be '") || startsWith("at least one constructor is required in common ") ||
            startsWith("only common/specific or Native FFI mirror abstract classes can have explicitly abstract ") ||
            startsWith("modifier '") || startsWith("an extend declaration must have exactly one ")
    }

    private fun isModifierSyntaxDiagnostic(diagnostic: ParsingDiagnostic): Boolean = with(diagnostic.message) {
        startsWith("modifier '") || startsWith("an extend declaration must have exactly one ")
    }

    private fun isPropertyAccessorSyntaxDiagnostic(diagnostic: ParsingDiagnostic): Boolean = with(diagnostic.message) {
        startsWith("expected 'get' or 'set' in prop body") ||
            startsWith("duplicated modifier: '") ||
            startsWith("duplicated 'get' in prop") ||
            startsWith("duplicated 'set' in prop") ||
            startsWith("unexpected annotation '") ||
            startsWith("expected no modifier before getter function") ||
            startsWith("expected no modifier before setter function")
    }

    private fun sorted(diagnostics: List<ParsingDiagnostic>): List<ParsingDiagnostic> =
        diagnostics.sortedWith(compareBy({ it.startOffset }, { it.endOffset }, { it.message }))

    private companion object {
        val COMMON = CjmpParsingContext.Compilation(true, false, true, parameterRoleMatching = true)
        val SPECIFIC = CjmpParsingContext.Compilation(false, true, true, parameterRoleMatching = true)
        val PLATFORM = CjmpParsingContext.Compilation(
            compileCommon = false,
            compileSpecific = true,
            enabled = true,
            genericDeclarationsAvailable = true,
            parameterDefaultValueRulesAvailable = true,
            modifierSyntaxAvailable = false,
            platformModifierSyntaxAvailable = true,
            primaryConstructorRoleModifiersAvailable = true,
        )
        val MIXED = CjmpParsingContext.Compilation(
            compileCommon = true,
            compileSpecific = true,
            enabled = true,
            mixedCommonSpecificDeclarations = true,
            commonMemberInSpecificOuter = true,
        )
        val MIXED_WITH_COMMON_NESTING_DISABLED = CjmpParsingContext.Compilation(
            compileCommon = true,
            compileSpecific = true,
            enabled = true,
            mixedCommonSpecificDeclarations = true,
        )
    }
}

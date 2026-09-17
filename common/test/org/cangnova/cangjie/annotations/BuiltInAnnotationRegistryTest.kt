package org.cangnova.cangjie.annotations

import org.cangnova.cangjie.name.FqName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 锁定官方 revision 896235c9fd18f22d570a9818ac672838c36c3932 的公共注解契约。
 *
 * 这些断言保护 parser、CFIR、CJO 和 Analysis API 共同消费的语言身份，不能用
 * 测试通过代替各语义 owner 的正负例验证。
 */
class BuiltInAnnotationRegistryTest {
    /** 24 个官方 kind 与 26 个源码拼写必须完整，系统注解不能混入枚举。 */
    @Test
    fun officialKindsAndSourceSpellingsAreComplete() {
        val expectedSourceNames = setOf(
            "Java", "JavaMirror", "JavaImpl", "JavaHasDefault", "ObjCMirror", "ObjCImpl",
            "ObjCInit", "ObjCOptional", "ForeignName", "ForeignGetterName", "ForeignSetterName",
            "CallingConv", "C", "Attribute", "Intrinsic", "OverflowThrowing", "OverflowWrapping",
            "OverflowSaturating", "When", "FastNative", "ConstSafe", "Annotation", "Deprecated",
            "Frozen", "EnsurePreparedToMock", "NonProduct",
        )
        val descriptors = BuiltInAnnotationRegistry.languageBuiltIns

        assertEquals(expectedSourceNames, descriptors.mapTo(linkedSetOf()) { it.sourceName })
        assertEquals(expectedSourceNames.size, descriptors.size)
        assertEquals(24, BuiltInAnnotationKind.entries.size)
        assertEquals(BuiltInAnnotationKind.entries.toSet(), descriptors.mapTo(linkedSetOf()) { it.kind })
        for (descriptor in descriptors) {
            assertSame(descriptor, BuiltInAnnotationRegistry.findLanguageBuiltIn(descriptor.sourceName))
            assertSame(descriptor, BuiltInAnnotationRegistry.find(descriptor.sourceName))
            assertEquals(CangjieAnnotationOrigin.LANGUAGE_BUILT_IN, descriptor.origin)
            assertFalse(descriptor.supportsCompileTimeVisibleForm, descriptor.sourceName)
        }
    }

    /** Overflow 的源码策略不能因为共享官方 kind 而丢失或合并。 */
    @Test
    fun overflowSpellingsRetainDistinctStrategies() {
        val expectedStrategies = mapOf(
            "OverflowThrowing" to CangjieOverflowStrategy.THROWING,
            "OverflowWrapping" to CangjieOverflowStrategy.WRAPPING,
            "OverflowSaturating" to CangjieOverflowStrategy.SATURATING,
        )
        for ((sourceName, strategy) in expectedStrategies) {
            val descriptor = builtIn(sourceName)
            assertEquals(BuiltInAnnotationKind.NUMERIC_OVERFLOW, descriptor.kind)
            assertEquals(strategy, descriptor.overflowStrategy)
            assertEquals(CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY, descriptor.argumentSyntax)
            assertEquals(AnnotationSemanticHandler.OVERFLOW, descriptor.semanticHandler)
            assertTrue(descriptor.allowsExpression)
            assertFalse(descriptor.repeatable)
        }
    }

    /** Java 是官方 parser 内置身份；ConstSafe 仍只在 std 模块内建。 */
    @Test
    fun sourceLookupHonorsSpecialJavaAndStdOnlyConstSafe() {
        val java = builtIn("Java")
        assertEquals(BuiltInAnnotationKind.JAVA, java.kind)
        assertTrue(java.hasSourceParserEntry)
        assertSame(java, BuiltInAnnotationRegistry.resolveLanguageBuiltIn("Java", forcedCustom = false, moduleName = "std"))
        assertSame(java, BuiltInAnnotationRegistry.resolveLanguageBuiltIn("Java", forcedCustom = false, moduleName = "application"))

        assertSame(builtIn("ConstSafe"), BuiltInAnnotationRegistry.resolveLanguageBuiltIn("ConstSafe", false, "std"))
        assertNull(BuiltInAnnotationRegistry.resolveLanguageBuiltIn("ConstSafe", false, "application"))
        assertNull(BuiltInAnnotationRegistry.resolveLanguageBuiltIn("ConstSafe", true, "std"))
        assertSame(builtIn("C"), BuiltInAnnotationRegistry.resolveLanguageBuiltIn("C", false, "application"))
        assertNull(BuiltInAnnotationRegistry.resolveLanguageBuiltIn("C", true, "application"))
        assertNull(BuiltInAnnotationRegistry.resolveLanguageBuiltIn("sample.C", false, "application"))
    }

    /** Deprecated 的第二个位置参数仍是重复 message，不能顺序分配到 since。 */
    @Test
    fun deprecatedSchemaHasOnePositionalMessageAndNamedSinceStrict() {
        val schema = builtIn("Deprecated").argumentSchema
        assertEquals(listOf("message", "since", "strict"), schema.parameters.map { it.name })
        assertEquals("message", schema.positionalParameter?.name)
        assertEquals(AnnotationParameterKind.STRING, schema.parameters.single { it.name == "message" }.kind)
        assertFalse(schema.parameters.single { it.name == "since" }.acceptsPositional)
        val strict = schema.parameters.single { it.name == "strict" }
        assertFalse(strict.acceptsPositional)
        assertEquals(AnnotationParameterKind.BOOLEAN, strict.kind)
        assertEquals(AnnotationDefaultValue.BooleanValue(false), strict.defaultValue)
        assertFalse(schema.acceptsArbitrarySingleName)
        assertFalse(schema.variadic)
    }

    /** 外部函数名与 accessor 名共享字符串类型，但只有 accessor 要求未命名实参。 */
    @Test
    fun foreignNameSchemasDistinguishAccessorArguments() {
        val name = builtIn("ForeignName").argumentSchema
        assertTrue(name.acceptsArbitrarySingleName)
        assertTrue(assertNotNull(name.positionalParameter).required)
        assertEquals(AnnotationParameterKind.STRING, name.positionalParameter?.kind)

        for (sourceName in listOf("ForeignGetterName", "ForeignSetterName")) {
            val descriptor = builtIn(sourceName)
            assertFalse(descriptor.argumentSchema.acceptsArbitrarySingleName)
            assertTrue(assertNotNull(descriptor.argumentSchema.positionalParameter).required)
            assertEquals(setOf(CangjieAnnotationTarget.MEMBER_PROPERTY), descriptor.declarationTargets)
            assertEquals(AnnotationSemanticHandler.FOREIGN_NAME, descriptor.semanticHandler)
        }
    }

    /** 十类 target 的源码名和官方 ABI 顺序必须稳定，最后一项源码拼写为 Extension。 */
    @Test
    fun annotationTargetsKeepOfficialNamesAndOrder() {
        val expectedNames = listOf(
            "Type", "Parameter", "Init", "MemberProperty", "MemberFunction", "MemberVariable",
            "EnumConstructor", "GlobalFunction", "GlobalVariable", "Extension",
        )
        val targets = CangjieAnnotationTarget.valuesInOfficialOrder()
        assertEquals(expectedNames, targets.map { it.sourceName })
        assertEquals((0..9).toList(), targets.map { it.bitPosition })
        assertTrue(targets.all { CangjieAnnotationTarget.ALL_TARGETS.matches(it) })
        val annotation = builtIn("Annotation")
        assertEquals(setOf(CangjieAnnotationTarget.TYPE), annotation.declarationTargets)
        val targetParameter = annotation.argumentSchema.parameters.single()
        assertEquals("target", targetParameter.name)
        assertEquals(AnnotationParameterKind.TARGET_ARRAY, targetParameter.kind)
        assertFalse(targetParameter.acceptsPositional)
        assertFalse(targetParameter.required)
    }

    /** 系统类注解与特殊表达式不属于官方 AnnotationKind，也不能靠短名冒充内置类型。 */
    @Test
    fun systemClassesAndIfAvailableStaySeparateFromLanguageKinds() {
        assertEquals(setOf("APILevel", "Hide", "IfAvailable"),
            BuiltInAnnotationRegistry.systemAndSpecial.mapTo(linkedSetOf()) { it.sourceName })
        for (descriptor in BuiltInAnnotationRegistry.systemAndSpecial) {
            assertNull(descriptor.kind)
            assertNull(BuiltInAnnotationRegistry.findLanguageBuiltIn(descriptor.sourceName))
            assertEquals(AnnotationSemanticHandler.AVAILABILITY, descriptor.semanticHandler)
        }

        val apiLevel = assertNotNull(BuiltInAnnotationRegistry.findSystemAnnotation(FqName("ohos.labels.APILevel")))
        assertEquals(CangjieAnnotationOrigin.SYSTEM_MACRO, apiLevel.origin)
        assertTrue(apiLevel.repeatable)
        assertTrue(apiLevel.supportsCompileTimeVisibleForm)
        assertNull(BuiltInAnnotationRegistry.findSystemAnnotation(FqName("application.APILevel")))

        val hide = assertNotNull(BuiltInAnnotationRegistry.findSystemAnnotation(FqName("ohos.labels.Hide")))
        assertFalse(hide.repeatable)
        assertTrue(hide.supportsCompileTimeVisibleForm)
        assertEquals(AnnotationDefaultValue.BooleanValue(false), hide.argumentSchema.parameters.single().defaultValue)

        val ifAvailable = BuiltInAnnotationRegistry.systemAndSpecial.single { it.sourceName == "IfAvailable" }
        assertNull(ifAvailable.classFqName)
        assertEquals(CangjieAnnotationOrigin.SPECIAL_EXPRESSION, ifAvailable.origin)
        assertEquals(CangjieAnnotationArgumentSyntax.SPECIAL_EXPRESSION, ifAvailable.argumentSyntax)
        assertFalse(ifAvailable.supportsCompileTimeVisibleForm)
        assertTrue(ifAvailable.declarationTargets.isEmpty())
    }

    /** FFI 互斥身份、函数目标和包级 NonProduct 必须由各自的语义 owner 消费。 */
    @Test
    fun ffiAndCompilerDirectivesKeepTheirSemanticOwners() {
        assertEquals(setOf(BuiltInAnnotationKind.C, BuiltInAnnotationKind.JAVA,
            BuiltInAnnotationKind.JAVA_MIRROR, BuiltInAnnotationKind.JAVA_IMPL,
            BuiltInAnnotationKind.OBJ_C_MIRROR, BuiltInAnnotationKind.OBJ_C_IMPL),
            BuiltInAnnotationRegistry.ffiExclusiveKinds)
        assertEquals(setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION), builtIn("CallingConv").declarationTargets)
        assertEquals(setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION), builtIn("FastNative").declarationTargets)
        assertEquals(setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION, CangjieAnnotationTarget.MEMBER_FUNCTION,
            CangjieAnnotationTarget.MEMBER_PROPERTY), builtIn("Frozen").declarationTargets)

        val mock = builtIn("EnsurePreparedToMock")
        assertEquals(AnnotationSemanticHandler.MOCK_PREPARATION, mock.semanticHandler)
        assertTrue(mock.allowsExpression)
        assertTrue(mock.declarationTargets.isEmpty())
        val nonProduct = builtIn("NonProduct")
        assertEquals(AnnotationSemanticHandler.PACKAGE_PRODUCT, nonProduct.semanticHandler)
        assertTrue(nonProduct.declarationTargets.isEmpty())
        assertEquals(CangjieAnnotationArgumentSyntax.NONE, nonProduct.argumentSyntax)
        assertEquals(AnnotationArgumentSchema.NONE, nonProduct.argumentSchema)
    }

    private fun builtIn(sourceName: String): BuiltInAnnotationDescriptor =
        assertNotNull(BuiltInAnnotationRegistry.findLanguageBuiltIn(sourceName), sourceName)
}

/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.annotations

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 官方仓颉 AST 中的注解 kind。
 *
 * 这个枚举只描述语言前端的身份，不描述某个 PSI 节点或某个后端属性。
 * 三个 Overflow 源码名称在官方 AST 中共享 [NUMERIC_OVERFLOW]，具体策略由
 * [BuiltInAnnotationDescriptor.overflowStrategy] 记录。
 */
public enum class BuiltInAnnotationKind {
    JAVA,
    CALLING_CONV,
    C,
    JAVA_MIRROR,
    JAVA_IMPL,
    JAVA_HAS_DEFAULT,
    OBJ_C_MIRROR,
    OBJ_C_IMPL,
    OBJ_C_INIT,
    OBJ_C_OPTIONAL,
    FOREIGN_NAME,
    FOREIGN_GETTER_NAME,
    FOREIGN_SETTER_NAME,
    ATTRIBUTE,
    NUMERIC_OVERFLOW,
    INTRINSIC,
    WHEN,
    FASTNATIVE,
    ANNOTATION,
    CONSTSAFE,
    DEPRECATED,
    FROZEN,
    ENSURE_PREPARED_TO_MOCK,
    NON_PRODUCT,
}

/** 注解来源层级；不能把不同来源压缩成一个 builtin 标志。 */
public enum class CangjieAnnotationOrigin {
    /** 官方语言 parser 直接识别的 AnnotationKind。 */
    LANGUAGE_BUILT_IN,

    /** 普通用户自定义注解。 */
    CUSTOM,

    /** 由系统库声明和宏处理器共同定义的注解语义。 */
    SYSTEM_MACRO,

    /** 不是注解调用，而是拥有独立 AST/CFIR 生命周期的特殊表达式。 */
    SPECIAL_EXPRESSION,

    /** Java/ObjC/CJMP 等阶段派生的互操作 metadata。 */
    PLATFORM_DERIVED,
}

/** 内置注解的参数语法类别。 */
public enum class CangjieAnnotationArgumentSyntax {
    NONE,
    OPTIONAL_SINGLE_STRING_LITERAL,
    SINGLE_STRING_LITERAL,
    CALLING_CONVENTION_REFERENCE,
    ATTRIBUTE_TOKENS,
    OVERFLOW_STRATEGY,
    WHEN_CONDITION,
    ANNOTATION_TARGET_ARRAY,
    CUSTOM_EXPRESSION,
    SPECIAL_EXPRESSION,
}

/** 数值 Overflow 的具体源码策略。 */
public enum class CangjieOverflowStrategy {
    NA,
    CHECKED,
    WRAPPING,
    THROWING,
    SATURATING,
}

/**
 * C function calling convention understood by the official frontend.
 *
 * This is part of the source-level interop contract shared by common, CFIR and
 * Analysis API.  It must remain a public, backend-neutral value; LLVM/JVM
 * calling-convention identifiers belong to backend adapters.
 */
public enum class CangjieCallingConvention {
    CDECL,
    STDCALL,
}

/**
 * 内置注解的唯一事实来源。
 *
 * 依据官方 896235c9fd18f22d570a9818ac672838c36c3932 的 ParseAnnotations、
 * CFFICheck 与 NativeFFI。目标集合描述十类声明目标，class/struct、作用域、
 * override 等更细约束由 semanticHandler 指定的语义 owner 检查。
 */
object BuiltInAnnotationRegistry {
    private val all = CangjieAnnotationTarget.entries.toSet()
    private val functions = setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION, CangjieAnnotationTarget.MEMBER_FUNCTION)
    private val types = setOf(CangjieAnnotationTarget.TYPE)
    private val nameSchema = AnnotationArgumentSchema(
        listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, acceptsPositional = true)),
        acceptsArbitrarySingleName = true,
    )
    private val requiredNameSchema = nameSchema.copy(
        parameters = nameSchema.parameters.map { it.copy(required = true) },
    )
    private val accessorNameSchema = requiredNameSchema.copy(acceptsArbitrarySingleName = false)
    private val unrestricted = AnnotationArgumentSchema(variadic = true)

    val languageBuiltIns: List<BuiltInAnnotationDescriptor> = listOf(
        // `Java` is present in the official NAME_TO_ANNO_KIND table and is
        // recognized by ParserImpl::SeeingBuiltinAnnotation.  Keep it in the
        // source parser registry; Java/ObjC interop implementation details are
        // resolved later, but the source-level annotation identity is still a
        // language builtin and must not fall through to a macro/custom path.
        ffi("Java", BuiltInAnnotationKind.JAVA, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL, nameSchema, types,
            AnnotationSemanticHandler.JAVA_FFI),
        ffi("JavaMirror", BuiltInAnnotationKind.JAVA_MIRROR, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL, nameSchema, types, AnnotationSemanticHandler.JAVA_FFI),
        ffi("JavaImpl", BuiltInAnnotationKind.JAVA_IMPL, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL, nameSchema, types, AnnotationSemanticHandler.JAVA_FFI),
        ffi("JavaHasDefault", BuiltInAnnotationKind.JAVA_HAS_DEFAULT, targets = setOf(CangjieAnnotationTarget.MEMBER_FUNCTION), handler = AnnotationSemanticHandler.JAVA_FFI),
        ffi("ObjCMirror", BuiltInAnnotationKind.OBJ_C_MIRROR, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL, nameSchema,
            types + CangjieAnnotationTarget.GLOBAL_FUNCTION, AnnotationSemanticHandler.OBJC_FFI),
        ffi("ObjCImpl", BuiltInAnnotationKind.OBJ_C_IMPL, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL, nameSchema, types, AnnotationSemanticHandler.OBJC_FFI),
        ffi("ObjCInit", BuiltInAnnotationKind.OBJ_C_INIT, targets = setOf(CangjieAnnotationTarget.MEMBER_FUNCTION), handler = AnnotationSemanticHandler.OBJC_FFI),
        ffi("ObjCOptional", BuiltInAnnotationKind.OBJ_C_OPTIONAL, targets = setOf(CangjieAnnotationTarget.MEMBER_FUNCTION), handler = AnnotationSemanticHandler.OBJC_FFI),
        ffi("ForeignName", BuiltInAnnotationKind.FOREIGN_NAME, CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL, requiredNameSchema,
            functions + setOf(CangjieAnnotationTarget.INIT, CangjieAnnotationTarget.MEMBER_PROPERTY, CangjieAnnotationTarget.MEMBER_VARIABLE, CangjieAnnotationTarget.GLOBAL_VARIABLE), AnnotationSemanticHandler.FOREIGN_NAME),
        ffi("ForeignGetterName", BuiltInAnnotationKind.FOREIGN_GETTER_NAME, CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL, accessorNameSchema,
            setOf(CangjieAnnotationTarget.MEMBER_PROPERTY), AnnotationSemanticHandler.FOREIGN_NAME),
        ffi("ForeignSetterName", BuiltInAnnotationKind.FOREIGN_SETTER_NAME, CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL, accessorNameSchema,
            setOf(CangjieAnnotationTarget.MEMBER_PROPERTY), AnnotationSemanticHandler.FOREIGN_NAME),
        ffi("CallingConv", BuiltInAnnotationKind.CALLING_CONV, CangjieAnnotationArgumentSyntax.CALLING_CONVENTION_REFERENCE,
            AnnotationArgumentSchema(listOf(AnnotationParameterSchema("convention", AnnotationParameterKind.REFERENCE, required = true, acceptsPositional = true)), acceptsArbitrarySingleName = true),
            setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION)),
        ffi("C", BuiltInAnnotationKind.C, targets = types + CangjieAnnotationTarget.GLOBAL_FUNCTION),
        directive("Attribute", BuiltInAnnotationKind.ATTRIBUTE, CangjieAnnotationArgumentSyntax.ATTRIBUTE_TOKENS, unrestricted, all, AnnotationSemanticHandler.ATTRIBUTES),
        directive("Intrinsic", BuiltInAnnotationKind.INTRINSIC, CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION, unrestricted,
            setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION), AnnotationSemanticHandler.INTRINSIC),
        overflow("OverflowThrowing", CangjieOverflowStrategy.THROWING),
        overflow("OverflowWrapping", CangjieOverflowStrategy.WRAPPING),
        overflow("OverflowSaturating", CangjieOverflowStrategy.SATURATING),
        directive("When", BuiltInAnnotationKind.WHEN, CangjieAnnotationArgumentSyntax.WHEN_CONDITION, unrestricted, all, AnnotationSemanticHandler.CONDITIONAL_COMPILATION),
        directive("FastNative", BuiltInAnnotationKind.FASTNATIVE, CangjieAnnotationArgumentSyntax.NONE, AnnotationArgumentSchema.NONE,
            setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION), AnnotationSemanticHandler.C_FFI),
        directive("ConstSafe", BuiltInAnnotationKind.CONSTSAFE, CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION, unrestricted, all,
            AnnotationSemanticHandler.CONST_EVALUATION).copy(standardLibraryOnly = true),
        BuiltInAnnotationDescriptor("Annotation", BuiltInAnnotationKind.ANNOTATION, BuiltInAnnotationCategory.META,
            CangjieAnnotationArgumentSyntax.ANNOTATION_TARGET_ARRAY,
            AnnotationArgumentSchema(listOf(AnnotationParameterSchema("target", AnnotationParameterKind.TARGET_ARRAY))),
            types, AnnotationSemanticHandler.ANNOTATION_TYPE),
        BuiltInAnnotationDescriptor("Deprecated", BuiltInAnnotationKind.DEPRECATED, BuiltInAnnotationCategory.SEMANTIC,
            CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION,
            AnnotationArgumentSchema(listOf(
                AnnotationParameterSchema("message", AnnotationParameterKind.STRING, acceptsPositional = true, defaultValue = AnnotationDefaultValue.StringValue("")),
                AnnotationParameterSchema("since", AnnotationParameterKind.STRING),
                AnnotationParameterSchema("strict", AnnotationParameterKind.BOOLEAN, defaultValue = AnnotationDefaultValue.BooleanValue(false)),
            )), all, AnnotationSemanticHandler.DEPRECATION),
        BuiltInAnnotationDescriptor("Frozen", BuiltInAnnotationKind.FROZEN, BuiltInAnnotationCategory.SEMANTIC,
            CangjieAnnotationArgumentSyntax.NONE, AnnotationArgumentSchema.NONE, functions + CangjieAnnotationTarget.MEMBER_PROPERTY, AnnotationSemanticHandler.C_FFI),
        BuiltInAnnotationDescriptor("EnsurePreparedToMock", BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK, BuiltInAnnotationCategory.TESTING,
            CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION, unrestricted, emptySet(), AnnotationSemanticHandler.MOCK_PREPARATION, allowsExpression = true),
        directive("NonProduct", BuiltInAnnotationKind.NON_PRODUCT, CangjieAnnotationArgumentSyntax.NONE, AnnotationArgumentSchema.NONE,
            emptySet(), AnnotationSemanticHandler.PACKAGE_PRODUCT),
    )

    /** 系统类身份不能加入官方 AnnotationKind；IfAvailable 保留独立表达式语法。 */
    public val systemAndSpecial: List<SystemAnnotationDescriptor> = listOf(
        SystemAnnotationDescriptor("APILevel", FqName("ohos.labels.APILevel"), CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION,
            AnnotationArgumentSchema(listOf(
                AnnotationParameterSchema("since", AnnotationParameterKind.STRING, required = true),
                AnnotationParameterSchema("syscap", AnnotationParameterKind.STRING, defaultValue = AnnotationDefaultValue.StringValue("")),
                AnnotationParameterSchema("level", AnnotationParameterKind.INTEGER, acceptsPositional = true),
            )), all, repeatable = true, supportsCompileTimeVisibleForm = true),
        SystemAnnotationDescriptor("Hide", FqName("ohos.labels.Hide"), CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION,
            AnnotationArgumentSchema(listOf(AnnotationParameterSchema("isChecked", AnnotationParameterKind.BOOLEAN,
                defaultValue = AnnotationDefaultValue.BooleanValue(false)))), all, repeatable = false, supportsCompileTimeVisibleForm = true),
        SystemAnnotationDescriptor("IfAvailable", null, CangjieAnnotationArgumentSyntax.SPECIAL_EXPRESSION,
            unrestricted, emptySet(), repeatable = true, supportsCompileTimeVisibleForm = false,
            origin = CangjieAnnotationOrigin.SPECIAL_EXPRESSION),
    )

    public val bySourceName: Map<String, AnnotationDescriptor> =
        (languageBuiltIns + systemAndSpecial).associateBy { it.sourceName }
    private val builtInsByName = languageBuiltIns.associateBy { it.sourceName }
    public val ffiExclusiveKinds: Set<BuiltInAnnotationKind> = setOf(
        BuiltInAnnotationKind.C, BuiltInAnnotationKind.JAVA, BuiltInAnnotationKind.JAVA_MIRROR,
        BuiltInAnnotationKind.JAVA_IMPL, BuiltInAnnotationKind.OBJ_C_MIRROR, BuiltInAnnotationKind.OBJ_C_IMPL,
    )

    public fun findLanguageBuiltIn(sourceName: String): BuiltInAnnotationDescriptor? = builtInsByName[sourceName]
    public fun find(sourceName: String): AnnotationDescriptor? = bySourceName[sourceName]
    public fun findSystemAnnotation(classFqName: FqName): SystemAnnotationDescriptor? =
        systemAndSpecial.singleOrNull { it.classFqName == classFqName }

    /** 只识别未限定、未强制 custom 且符合模块约束的语言注解。 */
    public fun resolveLanguageBuiltIn(sourceName: String, forcedCustom: Boolean, moduleName: String): BuiltInAnnotationDescriptor? =
        builtInsByName[sourceName]?.takeIf {
            !forcedCustom && it.hasSourceParserEntry && (!it.standardLibraryOnly || moduleName == "std")
        }

    /** 对齐官方 package prefixPaths[0]；组织名是前缀，单段 package 没有模块前缀。 */
    public fun sourceModuleName(packageFqName: FqName, organizationName: Name? = null): String {
        if (organizationName != null) return organizationName.asString()
        val segments = packageFqName.pathSegments()
        return if (segments.size > 1) segments.first().asString() else ""
    }

    private fun ffi(
        name: String, kind: BuiltInAnnotationKind,
        syntax: CangjieAnnotationArgumentSyntax = CangjieAnnotationArgumentSyntax.NONE,
        schema: AnnotationArgumentSchema = AnnotationArgumentSchema.NONE,
        targets: Set<CangjieAnnotationTarget>,
        handler: AnnotationSemanticHandler = AnnotationSemanticHandler.C_FFI,
    ): BuiltInAnnotationDescriptor = BuiltInAnnotationDescriptor(name, kind, BuiltInAnnotationCategory.FFI, syntax, schema, targets, handler)

    private fun directive(
        name: String, kind: BuiltInAnnotationKind, syntax: CangjieAnnotationArgumentSyntax,
        schema: AnnotationArgumentSchema, targets: Set<CangjieAnnotationTarget>, handler: AnnotationSemanticHandler,
    ): BuiltInAnnotationDescriptor = BuiltInAnnotationDescriptor(name, kind, BuiltInAnnotationCategory.COMPILER_DIRECTIVE, syntax, schema, targets, handler)

    private fun overflow(name: String, strategy: CangjieOverflowStrategy): BuiltInAnnotationDescriptor =
        directive(name, BuiltInAnnotationKind.NUMERIC_OVERFLOW, CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY,
            AnnotationArgumentSchema(listOf(AnnotationParameterSchema("strategy", AnnotationParameterKind.REFERENCE, acceptsPositional = true))),
            functions + CangjieAnnotationTarget.INIT, AnnotationSemanticHandler.OVERFLOW)
            .copy(overflowStrategy = strategy, allowsExpression = true)
}

/** 解析后的注解身份；短名仅允许作为 display name。 */
public sealed interface CangjieAnnotationIdentity {
    public data class LanguageBuiltIn(
        val kind: BuiltInAnnotationKind,
        val sourceName: String,
    ) : CangjieAnnotationIdentity

    public data class Custom(
        val classFqName: FqName?,
    ) : CangjieAnnotationIdentity

    public data class SystemMacro(
        val classFqName: FqName?,
        val sourceName: String,
    ) : CangjieAnnotationIdentity

    public data class SpecialExpression(
        val sourceName: String,
    ) : CangjieAnnotationIdentity

    public data class PlatformDerived(
        val sourceName: String,
    ) : CangjieAnnotationIdentity

    public data object Unknown : CangjieAnnotationIdentity
}

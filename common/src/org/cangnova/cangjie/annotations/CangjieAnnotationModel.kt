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

/**
 * 官方仓颉 AST 中的注解 kind。
 *
 * 这个枚举只描述语言前端的身份，不描述某个 PSI 节点或某个后端属性。
 * 三个 Overflow 源码名称在官方 AST 中共享 [NUMERIC_OVERFLOW]，具体策略由
 * [CangjieAnnotationDescriptor.overflowStrategy] 记录。
 */
public enum class CangjieAnnotationKind {
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

/** 官方 `@Annotation(target: [...])` 中使用的十类目标值。 */
public enum class CangjieAnnotationTargetValue {
    TYPE,
    PARAMETER,
    INIT,
    MEMBER_PROPERTY,
    MEMBER_FUNCTION,
    MEMBER_VARIABLE,
    ENUM_CONSTRUCTOR,
    GLOBAL_FUNCTION,
    GLOBAL_VARIABLE,
    EXTEND,
}

/** 实际声明可以承载注解的目标类别。与 [CangjieAnnotationTargetValue] 有意分离。 */
public enum class CangjieDeclarationAnnotationTarget {
    TYPE,
    PARAMETER,
    INIT,
    MEMBER_PROPERTY,
    MEMBER_FUNCTION,
    MEMBER_VARIABLE,
    ENUM_CONSTRUCTOR,
    GLOBAL_FUNCTION,
    GLOBAL_VARIABLE,
    EXTEND,
    EXPRESSION,
}

/** 数值 Overflow 的具体源码策略。 */
public enum class CangjieOverflowStrategy {
    NA,
    CHECKED,
    WRAPPING,
    THROWING,
    SATURATING,
}

/** C function calling convention understood by the official frontend. */
public enum class CangjieCallingConvention {
    CDECL,
    STDCALL,
}

/**
 * 一个内置注解的跨模块静态契约。
 *
 * 该类型不包含 PSI、CFIR 或 checker 引用，因此可以作为 parser、Raw CFIR、
 * resolve、checker 和 Analysis API 的共同身份输入。
 */
public data class CangjieAnnotationDescriptor(
    /** 源码中的单个名称，不含 `@`。 */
    val sourceName: String,
    /** 官方 AST kind。custom/system/special 项可为 null。 */
    val kind: CangjieAnnotationKind?,
    /** 该名称的真实语义来源。 */
    val origin: CangjieAnnotationOrigin,
    /** 语法层参数形状。 */
    val argumentSyntax: CangjieAnnotationArgumentSyntax,
    /** 合法声明目标；空集合表示由专用语义 owner 决定。 */
    val declarationTargets: Set<CangjieDeclarationAnnotationTarget> = emptySet(),
    /** 是否允许多个实例。 */
    val repeatable: Boolean = false,
    /** 是否可由 `@!` 表示为编译期可见 custom/macro surface。 */
    val supportsCompileTimeVisibleForm: Boolean = false,
    /** Overflow source spelling 对应的具体策略。 */
    val overflowStrategy: CangjieOverflowStrategy? = null,
)

/**
 * 官方和系统 annotation 的唯一 catalog。
 *
 * 名称表集中维护，但语义处理仍由各自 parser/resolve/checker owner 负责；
 * catalog 本身不承担语义推导。
 */
public object CangjieAnnotationCatalog {
    public val languageBuiltIns: List<CangjieAnnotationDescriptor> = listOf(
        descriptor("Java", CangjieAnnotationKind.JAVA, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL),
        descriptor("CallingConv", CangjieAnnotationKind.CALLING_CONV, CangjieAnnotationArgumentSyntax.CALLING_CONVENTION_REFERENCE),
        descriptor("C", CangjieAnnotationKind.C, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("JavaMirror", CangjieAnnotationKind.JAVA_MIRROR, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL),
        descriptor("JavaImpl", CangjieAnnotationKind.JAVA_IMPL, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL),
        descriptor("JavaHasDefault", CangjieAnnotationKind.JAVA_HAS_DEFAULT, CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION),
        descriptor("ObjCMirror", CangjieAnnotationKind.OBJ_C_MIRROR, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL),
        descriptor("ObjCImpl", CangjieAnnotationKind.OBJ_C_IMPL, CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL),
        descriptor("ObjCInit", CangjieAnnotationKind.OBJ_C_INIT, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("ObjCOptional", CangjieAnnotationKind.OBJ_C_OPTIONAL, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("ForeignName", CangjieAnnotationKind.FOREIGN_NAME, CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL),
        descriptor("ForeignGetterName", CangjieAnnotationKind.FOREIGN_GETTER_NAME, CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL),
        descriptor("ForeignSetterName", CangjieAnnotationKind.FOREIGN_SETTER_NAME, CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL),
        descriptor("Attribute", CangjieAnnotationKind.ATTRIBUTE, CangjieAnnotationArgumentSyntax.ATTRIBUTE_TOKENS),
        descriptor("OverflowThrowing", CangjieAnnotationKind.NUMERIC_OVERFLOW, CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY, overflowStrategy = CangjieOverflowStrategy.THROWING),
        descriptor("OverflowWrapping", CangjieAnnotationKind.NUMERIC_OVERFLOW, CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY, overflowStrategy = CangjieOverflowStrategy.WRAPPING),
        descriptor("OverflowSaturating", CangjieAnnotationKind.NUMERIC_OVERFLOW, CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY, overflowStrategy = CangjieOverflowStrategy.SATURATING),
        descriptor("Intrinsic", CangjieAnnotationKind.INTRINSIC, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("When", CangjieAnnotationKind.WHEN, CangjieAnnotationArgumentSyntax.WHEN_CONDITION),
        descriptor("FastNative", CangjieAnnotationKind.FASTNATIVE, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("Annotation", CangjieAnnotationKind.ANNOTATION, CangjieAnnotationArgumentSyntax.ANNOTATION_TARGET_ARRAY),
        descriptor("ConstSafe", CangjieAnnotationKind.CONSTSAFE, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("Deprecated", CangjieAnnotationKind.DEPRECATED, CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION),
        descriptor("Frozen", CangjieAnnotationKind.FROZEN, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("EnsurePreparedToMock", CangjieAnnotationKind.ENSURE_PREPARED_TO_MOCK, CangjieAnnotationArgumentSyntax.NONE),
        descriptor("NonProduct", CangjieAnnotationKind.NON_PRODUCT, CangjieAnnotationArgumentSyntax.NONE),
    )

    /** 官方 parser 不将这些名称当作普通 language builtin。 */
    public val systemAndSpecial: List<CangjieAnnotationDescriptor> = listOf(
        descriptor(
            sourceName = "APILevel",
            kind = null,
            syntax = CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION,
            origin = CangjieAnnotationOrigin.SYSTEM_MACRO,
            supportsCompileTimeVisibleForm = true,
        ),
        descriptor(
            sourceName = "Hide",
            kind = null,
            syntax = CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION,
            origin = CangjieAnnotationOrigin.SYSTEM_MACRO,
            supportsCompileTimeVisibleForm = true,
        ),
        descriptor(
            sourceName = "IfAvailable",
            kind = null,
            syntax = CangjieAnnotationArgumentSyntax.SPECIAL_EXPRESSION,
            origin = CangjieAnnotationOrigin.SPECIAL_EXPRESSION,
        ),
    )

    public val bySourceName: Map<String, CangjieAnnotationDescriptor> =
        (languageBuiltIns + systemAndSpecial).associateBy { it.sourceName }

    public fun findLanguageBuiltIn(sourceName: String): CangjieAnnotationDescriptor? =
        languageBuiltIns.firstOrNull { it.sourceName == sourceName }

    public fun find(sourceName: String): CangjieAnnotationDescriptor? = bySourceName[sourceName]

    private fun descriptor(
        sourceName: String,
        kind: CangjieAnnotationKind?,
        syntax: CangjieAnnotationArgumentSyntax,
        origin: CangjieAnnotationOrigin = CangjieAnnotationOrigin.LANGUAGE_BUILT_IN,
        overflowStrategy: CangjieOverflowStrategy? = null,
        supportsCompileTimeVisibleForm: Boolean = false,
    ): CangjieAnnotationDescriptor = CangjieAnnotationDescriptor(
        sourceName = sourceName,
        kind = kind,
        origin = origin,
        argumentSyntax = syntax,
        supportsCompileTimeVisibleForm = supportsCompileTimeVisibleForm,
        overflowStrategy = overflowStrategy,
    )
}

/** 解析后的注解身份；短名仅允许作为 display name。 */
public sealed interface CangjieAnnotationIdentity {
    public data class LanguageBuiltIn(
        val kind: CangjieAnnotationKind,
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

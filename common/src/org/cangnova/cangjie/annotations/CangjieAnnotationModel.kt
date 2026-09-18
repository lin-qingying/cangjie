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
    /** 官方 AST/二进制互操作节点使用的 Java kind；v1.0.0 源码 parser 不注册 @Java。 */
    JAVA,
    CALLING_CONV,
    C,
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
}

/** features/package 元数据的独立身份域，不属于官方声明 AnnotationKind。 */
public enum class CangjiePackageDirectiveKind {
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

    /** 文件/features/package metadata，不属于声明注解。 */
    PACKAGE_DIRECTIVE,

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
 * 依据官方 v1.0.0 的 ParseAnnotations、
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
        ffi("CallingConv", BuiltInAnnotationKind.CALLING_CONV, CangjieAnnotationArgumentSyntax.CALLING_CONVENTION_REFERENCE,
            AnnotationArgumentSchema(listOf(AnnotationParameterSchema("convention", AnnotationParameterKind.REFERENCE, required = true, acceptsPositional = true)), acceptsArbitrarySingleName = true),
            setOf(CangjieAnnotationTarget.GLOBAL_FUNCTION)),
        ffi("C", BuiltInAnnotationKind.C, targets = types + CangjieAnnotationTarget.GLOBAL_FUNCTION),
        directive("Attribute", BuiltInAnnotationKind.ATTRIBUTE, CangjieAnnotationArgumentSyntax.ATTRIBUTE_TOKENS, unrestricted, all, AnnotationSemanticHandler.ATTRIBUTES),
        // 官方 parser 只在被标注声明为函数时执行 intrinsic 专用检查；其它
        // 声明种类不经过通用 AnnotationTarget 机制。因此这里的空集合表示
        // “交给 intrinsic 专用 owner”，并不表示“该注解不允许使用”。
        directive("Intrinsic", BuiltInAnnotationKind.INTRINSIC, CangjieAnnotationArgumentSyntax.CUSTOM_EXPRESSION, unrestricted,
            emptySet(), AnnotationSemanticHandler.INTRINSIC),
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
    )

    /** Package/features metadata is deliberately outside the declaration builtin catalog. */
    public val packageDirectives: List<BuiltInAnnotationDescriptor> = listOf(
        directive("NonProduct", null, CangjieAnnotationArgumentSyntax.NONE,
            AnnotationArgumentSchema.NONE, emptySet(), AnnotationSemanticHandler.PACKAGE_PRODUCT)
            .copy(descriptorOrigin = CangjieAnnotationOrigin.PACKAGE_DIRECTIVE),
    )

    /**
     * 互操作库注解的源码契约。
     *
     * 这些名称由 `interoplib.interop`/`interoplib.objc` 提供的真实注解类解析，
     * 不是 v1.0.0 Parser.h::NAME_TO_ANNO_KIND 的语言 builtin。这里仅描述其
     * source surface 和后续语义 owner，不把它们伪造成 [BuiltInAnnotationKind]。
     */
    public val platformAnnotations: List<PlatformAnnotationDescriptor> by lazy {
        buildList {
            addAll(platformJavaAnnotations)
            addAll(platformObjCAnnotations)
        }
    }

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
        (languageBuiltIns + packageDirectives + systemAndSpecial).associateBy { it.sourceName }
    private val builtInsByName = languageBuiltIns.associateBy { it.sourceName }
    private val packageDirectivesByName = packageDirectives.associateBy { it.sourceName }
    /** 官方语言 builtin 中真正参与 C FFI ABI 互斥判断的 kind。 */
    public val ffiExclusiveKinds: Set<BuiltInAnnotationKind> = setOf(BuiltInAnnotationKind.C)

    /** 平台注解使用独立的身份域；消费者必须使用解析后的 ClassId 查询。 */
    public val platformInteropKinds: Set<CangjiePlatformAnnotationKind> by lazy {
        platformAnnotations.mapTo(linkedSetOf()) { it.platformKind }
    }

    public fun findLanguageBuiltIn(sourceName: String): BuiltInAnnotationDescriptor? = builtInsByName[sourceName]
    public fun findPackageDirective(sourceName: String): BuiltInAnnotationDescriptor? = packageDirectivesByName[sourceName]
    public fun find(sourceName: String): AnnotationDescriptor? = bySourceName[sourceName]
    public fun findSystemAnnotation(classFqName: FqName): SystemAnnotationDescriptor? =
        systemAndSpecial.singleOrNull { it.classFqName == classFqName }

    /** 仅按解析出的真实注解类 FqName 返回平台注解描述符。 */
    public fun findPlatformAnnotation(classFqName: FqName): PlatformAnnotationDescriptor? =
        platformAnnotations.singleOrNull { it.classFqName == classFqName }

    /** 仅供展示/目录校验使用；语义解析不得用源码短名调用此方法。 */
    public fun findPlatformAnnotationsBySourceName(sourceName: String): List<PlatformAnnotationDescriptor> =
        platformAnnotations.filter { it.sourceName == sourceName }

    /** 只识别未限定、未强制 custom 且符合模块约束的语言注解。 */
    public fun resolveLanguageBuiltIn(sourceName: String, forcedCustom: Boolean, moduleName: String): BuiltInAnnotationDescriptor? =
        builtInsByName[sourceName]?.takeIf {
            !forcedCustom && it.hasSourceParserEntry && (!it.standardLibraryOnly || moduleName == "std")
        }

    /** Resolve package/features metadata without promoting it to a declaration builtin. */
    public fun resolvePackageDirective(sourceName: String, forcedCustom: Boolean): BuiltInAnnotationDescriptor? =
        packageDirectivesByName[sourceName]?.takeUnless { forcedCustom }

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
        name: String, kind: BuiltInAnnotationKind?, syntax: CangjieAnnotationArgumentSyntax,
        schema: AnnotationArgumentSchema, targets: Set<CangjieAnnotationTarget>, handler: AnnotationSemanticHandler,
    ): BuiltInAnnotationDescriptor = BuiltInAnnotationDescriptor(name, kind, BuiltInAnnotationCategory.COMPILER_DIRECTIVE, syntax, schema, targets, handler)

    private fun overflow(name: String, strategy: CangjieOverflowStrategy): BuiltInAnnotationDescriptor =
        directive(name, BuiltInAnnotationKind.NUMERIC_OVERFLOW, CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY,
            AnnotationArgumentSchema(listOf(AnnotationParameterSchema("strategy", AnnotationParameterKind.REFERENCE, acceptsPositional = true))),
            functions + CangjieAnnotationTarget.INIT, AnnotationSemanticHandler.OVERFLOW)
            .copy(overflowStrategy = strategy, allowsExpression = true)
}

/** Java 互操作注解所在的官方包名（`interoplib.interop`）。 */
private val interoplibInterop = FqName("interoplib.interop")

/** ObjC 互操作注解所在的官方包名（`objc.lang`）。 */
private val interoplibObjc = FqName("objc.lang")

/**
 * Java 互操作平台注解的官方目录。
 *
 * 每项描述一个平台注解的源码拼写、参数形态、允许的声明目标和语义处理器；
 * 注解身份按 packageFqName + sourceName 判定，不走自定义注解解析路径。
 */
private val platformJavaAnnotations = listOf(
    PlatformAnnotationDescriptor(
        sourceName = "JavaMirror",
        platformKind = CangjiePlatformAnnotationKind.JAVA_MIRROR,
        packageFqName = interoplibInterop,
        argumentSyntax = CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, acceptsPositional = true)),
        ),
        declarationTargets = setOf(CangjieAnnotationTarget.TYPE),
        semanticHandler = AnnotationSemanticHandler.JAVA_FFI,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "JavaImpl",
        platformKind = CangjiePlatformAnnotationKind.JAVA_IMPL,
        packageFqName = interoplibInterop,
        argumentSyntax = CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, acceptsPositional = true)),
        ),
        declarationTargets = setOf(CangjieAnnotationTarget.TYPE),
        semanticHandler = AnnotationSemanticHandler.JAVA_FFI,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "JavaHasDefault",
        platformKind = CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT,
        packageFqName = interoplibInterop,
        argumentSyntax = CangjieAnnotationArgumentSyntax.NONE,
        argumentSchema = AnnotationArgumentSchema.NONE,
        declarationTargets = setOf(CangjieAnnotationTarget.MEMBER_FUNCTION),
        semanticHandler = AnnotationSemanticHandler.JAVA_FFI,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "ForeignName",
        platformKind = CangjiePlatformAnnotationKind.FOREIGN_NAME,
        packageFqName = interoplibInterop,
        argumentSyntax = CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, required = true, acceptsPositional = true)),
        ),
        declarationTargets = setOf(
            CangjieAnnotationTarget.MEMBER_FUNCTION,
        ),
        semanticHandler = AnnotationSemanticHandler.FOREIGN_NAME,
    ),
)

/**
 * ObjC 互操作平台注解的官方目录。
 *
 * 与 [platformJavaAnnotations] 同构：描述 ObjCMirror/ObjCImpl/ObjCInit 等
 * 注解的拼写、参数形态、声明目标与语义处理器；身份按 `objc.lang` 包 +
 * 源码拼写判定。
 */
private val platformObjCAnnotations = listOf(
    PlatformAnnotationDescriptor(
        sourceName = "ObjCMirror",
        platformKind = CangjiePlatformAnnotationKind.OBJ_C_MIRROR,
        packageFqName = interoplibObjc,
        argumentSyntax = CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, acceptsPositional = true)),
        ),
        declarationTargets = setOf(CangjieAnnotationTarget.TYPE, CangjieAnnotationTarget.GLOBAL_FUNCTION),
        semanticHandler = AnnotationSemanticHandler.OBJC_FFI,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "ObjCImpl",
        platformKind = CangjiePlatformAnnotationKind.OBJ_C_IMPL,
        packageFqName = interoplibObjc,
        argumentSyntax = CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, acceptsPositional = true)),
        ),
        declarationTargets = setOf(CangjieAnnotationTarget.TYPE),
        semanticHandler = AnnotationSemanticHandler.OBJC_FFI,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "ObjCInit",
        platformKind = CangjiePlatformAnnotationKind.OBJ_C_INIT,
        packageFqName = interoplibObjc,
        // 官方 ObjC 互操作语法允许省略 selector，也允许提供一个字符串
        // selector；第二个实参和非字符串实参由参数 owner 拒绝。
        argumentSyntax = CangjieAnnotationArgumentSyntax.OPTIONAL_SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, acceptsPositional = true)),
        ),
        declarationTargets = setOf(CangjieAnnotationTarget.MEMBER_FUNCTION),
        semanticHandler = AnnotationSemanticHandler.OBJC_FFI,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "ObjCOptional",
        platformKind = CangjiePlatformAnnotationKind.OBJ_C_OPTIONAL,
        packageFqName = interoplibObjc,
        argumentSyntax = CangjieAnnotationArgumentSyntax.NONE,
        argumentSchema = AnnotationArgumentSchema.NONE,
        declarationTargets = setOf(CangjieAnnotationTarget.MEMBER_FUNCTION),
        semanticHandler = AnnotationSemanticHandler.OBJC_FFI,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "ForeignName",
        platformKind = CangjiePlatformAnnotationKind.FOREIGN_NAME,
        packageFqName = interoplibObjc,
        argumentSyntax = CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, required = true, acceptsPositional = true)),
        ),
        declarationTargets = setOf(
            CangjieAnnotationTarget.INIT,
            CangjieAnnotationTarget.MEMBER_PROPERTY,
            CangjieAnnotationTarget.MEMBER_FUNCTION,
        ),
        semanticHandler = AnnotationSemanticHandler.FOREIGN_NAME,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "ForeignGetterName",
        platformKind = CangjiePlatformAnnotationKind.FOREIGN_GETTER_NAME,
        packageFqName = interoplibObjc,
        argumentSyntax = CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, required = true, acceptsPositional = true)),
        ),
        declarationTargets = setOf(CangjieAnnotationTarget.MEMBER_PROPERTY),
        semanticHandler = AnnotationSemanticHandler.FOREIGN_NAME,
    ),
    PlatformAnnotationDescriptor(
        sourceName = "ForeignSetterName",
        platformKind = CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME,
        packageFqName = interoplibObjc,
        argumentSyntax = CangjieAnnotationArgumentSyntax.SINGLE_STRING_LITERAL,
        argumentSchema = AnnotationArgumentSchema(
            listOf(AnnotationParameterSchema("name", AnnotationParameterKind.STRING, required = true, acceptsPositional = true)),
        ),
        declarationTargets = setOf(CangjieAnnotationTarget.MEMBER_PROPERTY),
        semanticHandler = AnnotationSemanticHandler.FOREIGN_NAME,
    ),
)

/** 解析后的注解身份；短名仅允许作为 display name。 */
public sealed interface CangjieAnnotationIdentity {
    public data class LanguageBuiltIn(
        val kind: BuiltInAnnotationKind,
        val sourceName: String,
    ) : CangjieAnnotationIdentity

    public data class PackageDirective(
        val kind: CangjiePackageDirectiveKind,
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
        val kind: CangjiePlatformAnnotationKind,
        val classFqName: FqName,
        val sourceName: String,
    ) : CangjieAnnotationIdentity

    public data object Unknown : CangjieAnnotationIdentity
}

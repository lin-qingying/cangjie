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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.annotations.BuiltInAnnotationCategory
import org.cangnova.cangjie.annotations.BuiltInAnnotationDescriptor
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry

/**
 * PSI 兼容 facade for the common annotation identity.
 *
 * 语义事实只保存在 common 的 [BuiltInAnnotationKind] 和 descriptor 中；本类型只保留
 * 旧 PSI API 的常量、显示属性以及名称查询，禁止在 PSI 再维护一套 builtin kind 枚举。
 * Java/ObjC 等平台注解如果由 registry 描述，仍通过 descriptor 投影，但不会产生第二个
 * kind 身份。
 */
class CjBuiltInAnnotation private constructor(
    /** common 层发布的静态描述符。 */
    val descriptor: BuiltInAnnotationDescriptor,
) {
    /** 源码中的注解名（不含 `@`）。 */
    val annotationName: String get() = descriptor.sourceName

    /** 兼容旧 PSI API 的描述文本。 */
    val description: String get() = descriptor.sourceName

    /** 兼容旧 PSI API 的分类投影。 */
    val category: AnnotationCategory
        get() = when (descriptor.category) {
            BuiltInAnnotationCategory.FFI -> AnnotationCategory.FFI
            BuiltInAnnotationCategory.COMPILER_DIRECTIVE -> AnnotationCategory.COMPILER_DIRECTIVE
            BuiltInAnnotationCategory.SEMANTIC -> AnnotationCategory.SEMANTIC
            BuiltInAnnotationCategory.META -> AnnotationCategory.META
            BuiltInAnnotationCategory.TESTING -> AnnotationCategory.TESTING
            BuiltInAnnotationCategory.SYSTEM -> AnnotationCategory.COMPILER_DIRECTIVE
        }

    /** common 层唯一的官方/平台 kind 投影。 */
    val kind: BuiltInAnnotationKind get() = descriptor.kind

    override fun toString(): String = annotationName

    companion object {
        private val byName: Map<String, CjBuiltInAnnotation> =
            BuiltInAnnotationRegistry.languageBuiltIns
                .associate { descriptor -> descriptor.sourceName to CjBuiltInAnnotation(descriptor) }

        /** 兼容旧 API 的全部注册项，顺序与 common registry 一致。 */
        @JvmField
        val entries: List<CjBuiltInAnnotation> = byName.values.toList()

        @JvmField val JAVA: CjBuiltInAnnotation = byName.require("Java")
        @JvmField val CALLING_CONV: CjBuiltInAnnotation = byName.require("CallingConv")
        @JvmField val C: CjBuiltInAnnotation = byName.require("C")
        @JvmField val JAVA_MIRROR: CjBuiltInAnnotation = byName.require("JavaMirror")
        @JvmField val JAVA_IMPL: CjBuiltInAnnotation = byName.require("JavaImpl")
        @JvmField val JAVA_HAS_DEFAULT: CjBuiltInAnnotation = byName.require("JavaHasDefault")
        @JvmField val OBJ_C_MIRROR: CjBuiltInAnnotation = byName.require("ObjCMirror")
        @JvmField val OBJ_C_IMPL: CjBuiltInAnnotation = byName.require("ObjCImpl")
        @JvmField val OBJ_C_INIT: CjBuiltInAnnotation = byName.require("ObjCInit")
        @JvmField val OBJ_C_OPTIONAL: CjBuiltInAnnotation = byName.require("ObjCOptional")
        @JvmField val FOREIGN_NAME: CjBuiltInAnnotation = byName.require("ForeignName")
        @JvmField val FOREIGN_GETTER_NAME: CjBuiltInAnnotation = byName.require("ForeignGetterName")
        @JvmField val FOREIGN_SETTER_NAME: CjBuiltInAnnotation = byName.require("ForeignSetterName")
        @JvmField val ATTRIBUTE: CjBuiltInAnnotation = byName.require("Attribute")
        @JvmField val OVERFLOW_THROWING: CjBuiltInAnnotation = byName.require("OverflowThrowing")
        @JvmField val OVERFLOW_WRAPPING: CjBuiltInAnnotation = byName.require("OverflowWrapping")
        @JvmField val OVERFLOW_SATURATING: CjBuiltInAnnotation = byName.require("OverflowSaturating")
        @JvmField val INTRINSIC: CjBuiltInAnnotation = byName.require("Intrinsic")
        @JvmField val WHEN: CjBuiltInAnnotation = byName.require("When")
        @JvmField val FAST_NATIVE: CjBuiltInAnnotation = byName.require("FastNative")
        @JvmField val ANNOTATION: CjBuiltInAnnotation = byName.require("Annotation")
        @JvmField val CONST_SAFE: CjBuiltInAnnotation = byName.require("ConstSafe")
        @JvmField val DEPRECATED: CjBuiltInAnnotation = byName.require("Deprecated")
        @JvmField val FROZEN: CjBuiltInAnnotation = byName.require("Frozen")
        @JvmField val ENSURE_PREPARED_TO_MOCK: CjBuiltInAnnotation = byName.require("EnsurePreparedToMock")
        @JvmField val NON_PRODUCT: CjBuiltInAnnotation = byName.require("NonProduct")

        /** 所有注解源码名称。 */
        @JvmField
        val ALL_NAMES: Set<String> = byName.keys

        /** 按源码名获取 common registry 的 facade。 */
        @JvmStatic
        fun fromName(name: String): CjBuiltInAnnotation? = byName[name]

        /** 判断名称是否由 common registry 声明。 */
        @JvmStatic
        fun isBuiltIn(name: String): Boolean = BuiltInAnnotationRegistry.findLanguageBuiltIn(name) != null

        /** 按兼容分类过滤 registry 项。 */
        @JvmStatic
        fun byCategory(category: AnnotationCategory): List<CjBuiltInAnnotation> =
            entries.filter { it.category == category }

        private fun Map<String, CjBuiltInAnnotation>.require(name: String): CjBuiltInAnnotation =
            getValue(name)
    }
}

/** 注解分类兼容投影；语义分类由 common registry 负责。 */
enum class AnnotationCategory(val description: String) {
    FFI("外部函数接口"),
    COMPILER_DIRECTIVE("编译器指令"),
    SEMANTIC("语义标记"),
    META("元注解"),
    TESTING("测试"),
}

/** 旧 PSI API 的 C 调用约定投影。 */
enum class CallingConvention(
    val conventionName: String,
    val description: String,
) {
    CDECL("CDECL", "C语言默认调用约定"),
    STDCALL("STDCALL", "Win32 API调用约定"),
    ;

    companion object {
        val ALL_CONVENTION_NAMES: Set<String> = entries.map { it.conventionName }.toSet()

        fun fromName(name: String): CallingConvention? = entries.find { it.conventionName == name }

        fun isValid(name: String): Boolean = name in ALL_CONVENTION_NAMES
    }
}

/** `@Annotation` 的目标名称兼容投影。 */
enum class CjAnnotationTarget(
    val targetName: String,
    val description: String,
) {
    TYPE("TYPE", "类型声明"),
    PARAMETER("PARAMETER", "参数声明"),
    INIT("INIT", "初始化块"),
    MEMBER_PROPERTY("MEMBER_PROPERTY", "成员属性"),
    MEMBER_FUNCTION("MEMBER_FUNCTION", "成员函数"),
    MEMBER_VARIABLE("MEMBER_VARIABLE", "成员变量"),
    ENUM_CONSTRUCTOR("ENUM_CONSTRUCTOR", "枚举构造函数"),
    GLOBAL_FUNCTION("GLOBAL_FUNCTION", "全局函数"),
    GLOBAL_VARIABLE("GLOBAL_VARIABLE", "全局变量"),
    EXTEND("EXTEND", "扩展声明"),
    ;

    companion object {
        val ALL_TARGET_NAMES: Set<String> = entries.map { it.targetName }.toSet()

        fun fromName(name: String): CjAnnotationTarget? = entries.find { it.targetName == name }

        fun isValidTarget(name: String): Boolean = name in ALL_TARGET_NAMES
    }
}

/** 旧 PSI API 的溢出策略投影。 */
enum class OverflowStrategy(
    val strategyName: String,
    val description: String,
) {
    NA("no", "无溢出策略"),
    CHECKED("checked", "检查溢出"),
    WRAPPING("wrapping", "溢出环绕"),
    THROWING("throwing", "溢出抛出异常"),
    SATURATING("saturating", "溢出饱和"),
    ;

    companion object {
        val ALL_STRATEGY_NAMES: Set<String> = entries.map { it.strategyName }.toSet()

        fun fromName(name: String): OverflowStrategy? = entries.find { it.strategyName == name.lowercase() }

        fun isValid(name: String): Boolean = name.lowercase() in ALL_STRATEGY_NAMES
    }
}

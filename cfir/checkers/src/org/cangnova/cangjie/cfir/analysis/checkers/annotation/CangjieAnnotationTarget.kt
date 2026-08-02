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
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.annotation

/**
 * 仓颉 `@Annotation` 的官方目标分类。
 *
 * 该枚举与官方编译器 `external/cangjie_compiler/include/cangjie/AST/Node.h` 中
 * `enum class AnnotationTarget` 的 10 项**逐字对齐**，描述字符串与
 * `external/cangjie_compiler/src/CHIR/Checker/AnnotationChecker.cpp` 中
 * `ANNOTATION_TARGET_2_STRING` 一一对应。
 *
 * 仓颉与 Kotlin 不同：**编译器内部目标 = 用户语言层目标**，不存在超集。
 * 用户在 `@Annotation(target: [...])` 中填写的值即下方 10 项之一，无细分。
 *
 * @property description 诊断消息中展示的目标描述，与官方 `ANNOTATION_TARGET_2_STRING` 对齐。
 */
public enum class CangjieAnnotationTarget(val description: String) {
    /** 类型声明本身（class / struct / interface / enum 头）。 */
    TYPE("type"),

    /** 函数参数。 */
    PARAMETER("parameter"),

    /** 构造器（init）。 */
    INIT("init"),

    /** 成员 property。 */
    MEMBER_PROPERTY("member property"),

    /** 成员函数。 */
    MEMBER_FUNCTION("member function"),

    /** 成员变量。 */
    MEMBER_VARIABLE("member variable"),

    /** enum 构造器。 */
    ENUM_CONSTRUCTOR("enum constructor"),

    /** 顶层函数。 */
    GLOBAL_FUNCTION("global function"),

    /** 顶层变量。 */
    GLOBAL_VARIABLE("global variable"),

    /** extend 声明本身。 */
    EXTEND("extend"),
    ;

    /**
     * 该目标在位掩码中的位移。
     *
     * 与官方 `AnnotationTargetT` 的 `enum class` 隐式序号一致，从 0 起按声明顺序递增。
     */
    public val bitPosition: Int get() = ordinal

    public companion object {
        /**
         * 所有目标的位掩码全集，对应官方 `Annotation::EnableAllTargets()` 写入的 `~0u`。
         *
         * 用于"未显式声明 `@Annotation(target: ...)`"时默认允许全部目标的语义。
         */
        public val ALL_TARGETS: AnnotationTargetSet = AnnotationTargetSet(entries.toSet())

        /**
         * 按官方 `ANNOTATION_TARGET_2_STRING` 序返回所有目标，供诊断序列化对齐。
         */
        public fun valuesInOfficialOrder(): List<CangjieAnnotationTarget> = entries.toList()
    }
}

/**
 * `@Annotation` 目标的位掩码集合。
 *
 * 对应官方编译器 `Annotation::target`（`uint16_t`）与
 * `TestTarget(t)` / `EnableTarget(t)` / `EnableAllTargets()` 的位运算语义。
 *
 * 仓颉官方仅 10 个目标，`uint16_t` 容量充足；此处用 `Set` 封装以提升可读性，
 * 同时通过 [matches] 单 API 对外提供与官方 `TestTarget` 一致的判定接口。
 */
public class AnnotationTargetSet internal constructor(
    /** 被启用的目标集合；`null` 表示全启用（对应官方 `~0u`）。 */
    private val enabled: Set<CangjieAnnotationTarget>?,
) {
    /**
     * 是否启用全部目标（对应官方 `EnableAllTargets()` 后的状态）。
     */
    public val isAll: Boolean get() = enabled == null

    /**
     * 判定给定目标是否被本集合允许，对应官方 `Annotation::TestTarget(t)`。
     */
    public fun matches(target: CangjieAnnotationTarget): Boolean =
        enabled?.contains(target) ?: true

    /**
     * 判定本集合是否与另一集合存在任意共同允许目标。
     */
    public fun anyMatch(other: AnnotationTargetSet): Boolean {
        if (isAll || other.isAll) return true
        return enabled!!.any(other.enabled!!::contains)
    }

    override fun toString(): String =
        if (isAll) "AnnotationTargetSet(ALL)" else "AnnotationTargetSet(${enabled!!.joinToString(", ") { it.name }})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnnotationTargetSet) return false
        return enabled == other.enabled
    }

    override fun hashCode(): Int = enabled?.hashCode() ?: -1

    public companion object {
        /** 创建一个仅允许给定目标的集合。 */
        public fun of(targets: Iterable<CangjieAnnotationTarget>): AnnotationTargetSet =
            AnnotationTargetSet(targets.toList().toSet())

        /** 创建一个仅允许给定目标的集合。 */
        public fun of(target: CangjieAnnotationTarget, vararg others: CangjieAnnotationTarget): AnnotationTargetSet =
            AnnotationTargetSet(setOf(target, *others))

        /** 创建一个启用全部目标的集合，对应官方 `EnableAllTargets()`。 */
        public fun all(): AnnotationTargetSet = AnnotationTargetSet(null)

        /** 创建一个空集合，对应官方 `target = 0`（未启用任何目标）。 */
        public fun empty(): AnnotationTargetSet = AnnotationTargetSet(emptySet())
    }
}

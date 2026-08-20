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

package org.cangnova.cangjie.cfir.analysis.checkers.modifier

/**
 * 仓颉声明种类分类，用于修饰符目标判定。
 *
 * 与注解定位体系 `CangjieAnnotationTarget`（对齐官方 10 项）不同——
 * **修饰符的编译器内部需要细分**，本枚举承担"声明种类"这一维度，
 * 与 [Site]（"作用位置"维度）正交，组合成 [ModifierTarget]。
 *
 * 项集来源：
 * - 与官方 `external/cangjie_compiler/include/cangjie/AST/Node.h` 的
 *   `enum class AnnotationTarget` 10 项在粒度上对齐（TYPE/PARAMETER/INIT/...）；
 * - 补充修饰符判定需要的种类：`TYPE_PARAMETER`/`MACRO`/`TYPEALIAS`/`FILE`/`STATIC_INITIALIZER`/
 *   `PROPERTY_GETTER`/`PROPERTY_SETTER`/`LOCAL_CLASS`/`LAMBDA`/`TYPE_PROJECTION`。
 *
 * @property description 诊断消息中展示的种类描述。
 */
public enum class DeclarationKind(val description: String) {
    /** class 声明头。 */
    CLASS("class"),

    /** struct 声明头。 */
    STRUCT("struct"),

    /** interface 声明头。 */
    INTERFACE("interface"),

    /** enum 声明头。 */
    ENUM("enum"),

    /** extend 声明头。 */
    EXTEND("extend"),

    /** 函数（顶层或成员，按 [Site] 区分）。 */
    FUNCTION("function"),

    /** property（成员或顶层，按 [Site] 区分）。 */
    PROPERTY("property"),

    /** 变量（成员或顶层，按 [Site] 区分）。 */
    VARIABLE("variable"),

    /** 值参数。 */
    VALUE_PARAMETER("value parameter"),

    /** 构造器（init）。 */
    CONSTRUCTOR("constructor"),

    /** 静态初始化器。 */
    STATIC_INITIALIZER("static initializer"),

    /** enum 构造器。 */
    ENUM_CONSTRUCTOR("enum constructor"),

    /** macro 声明。 */
    MACRO("macro"),

    /** 类型别名。 */
    TYPEALIAS("typealias"),

    /** 类型参数。 */
    TYPE_PARAMETER("type parameter"),

    /** 文件。 */
    FILE("file"),

    /** property getter。 */
    PROPERTY_GETTER("getter"),

    /** property setter。 */
    PROPERTY_SETTER("setter"),

    /** 局部类。 */
    LOCAL_CLASS("local class"),

    /** lambda 表达式 / 匿名函数。 */
    LAMBDA("lambda"),

    /** 类型投影（`in`/`out` 等）。 */
    TYPE_PROJECTION("type projection"),
    ;

    public companion object {
        /** class-like 种类集合：class / struct / interface / enum（不含 extend）。 */
        public val CLASS_LIKE: Set<DeclarationKind> = setOf(CLASS, STRUCT, INTERFACE, ENUM)

        /** extend 也算 class-like 容器，与 class/struct/interface/enum 共享成员语义。 */
        public val CLASS_LIKE_OR_EXTEND: Set<DeclarationKind> = setOf(CLASS, STRUCT, INTERFACE, ENUM, EXTEND)
    }
}

/**
 * 修饰符作用位置分类，用于修饰符目标判定。
 *
 * 与 [DeclarationKind] 正交，组合成 [ModifierTarget]。
 * 仓颉不像 Kotlin 那样在一维枚举里预生成笛卡尔积子项（`CLASS_MEMBER_FUNCTION`/`STRUCT_MEMBER_FUNCTION`/...），
 * 而是用 `(DeclarationKind, Site)` 二维表达——例如"struct 成员函数"即 `(FUNCTION, MEMBER)` 蕴含的"struct 头"由上下文另行携带。
 *
 * 当前实现简化为三种位置；后续若需区分"顶层头"与"文件"，可扩 `TOP_LEVEL`。
 */
public enum class Site {
    /** 声明头本身（class 头、struct 头、extend 头、顶层函数头、...）。 */
    HEAD,

    /** 类型容器内的成员（class 内的 func/prop/var、struct 内的 func/prop、...）。 */
    MEMBER,

    /** 局部声明（局部函数、局部变量、局部类、lambda）。 */
    LOCAL,
    ;

    public companion object {
        /** 所有位置集合，用于"任意位置都允许"的修饰符谓词。 */
        public val ALL: Set<Site> = entries.toSet()
    }
}

/**
 * 修饰符目标：声明种类 + 作用位置的二维组合。
 *
 * 替代旧 `CangJieTarget` 一维枚举的笛卡尔积子项。
 * 例如"class 成员函数"在旧枚举里是 `CLASS_MEMBER_FUNCTION` 一项，
 * 在二维模型里由 `(FUNCTION, MEMBER)` + 上下文 class 容器共同表达。
 *
 * @property kind 声明种类。
 * @property site 作用位置。
 * @property container 当 site == MEMBER 时，承载该成员的 class-like 容器种类；否则为 `null`。
 *                     用于"class 成员"与"struct 成员"修饰符规则不同的场景。
 *                     简化判定可不携带——谓词默认忽略容器细分。
 */
public data class ModifierTarget(
    val kind: DeclarationKind,
    val site: Site,
    val container: DeclarationKind? = null,
) {
    /** 是否为成员声明。 */
    public val isMember: Boolean get() = site == Site.MEMBER

    /** 是否为声明头本身。 */
    public val isHead: Boolean get() = site == Site.HEAD

    /** 是否为局部声明。 */
    public val isLocal: Boolean get() = site == Site.LOCAL

    public companion object {
        /** 创建一个声明头目标。 */
        public fun head(kind: DeclarationKind): ModifierTarget = ModifierTarget(kind, Site.HEAD)

        /** 创建一个成员目标，可携带容器种类。 */
        public fun member(kind: DeclarationKind, container: DeclarationKind? = null): ModifierTarget =
            ModifierTarget(kind, Site.MEMBER, container)

        /** 创建一个局部目标。 */
        public fun local(kind: DeclarationKind): ModifierTarget = ModifierTarget(kind, Site.LOCAL)
    }
}

/**
 * 修饰符目标谓词：判定一个 [ModifierTarget] 是否被某修饰符允许。
 *
 * 替代旧 `Map<CjKeywordToken, Set<CangJieTarget>>` 的静态集合交集判定，
 * 改为谓词化以便支持"任意 class-like 的成员"这类共享常量组合，
 * 避免多张表手工列举笛卡尔积子项产生不一致。
 *
 * 与 Kotlin FIR 的 `TargetAllowedPredicate` 语义对齐，但保留按
 * `LanguageVersionSettings` 分支的扩展点（当前未使用）。
 */
public fun interface ModifierTargetPredicate {
    /**
     * 判定给定目标是否被该谓词允许。
     *
     * @param target 实际推导出的修饰符目标。
     * @param languageVersionSettings 语言版本设置，预留按版本分支的扩展点。
     */
    public fun isAllowed(
        target: ModifierTarget,
        languageVersionSettings: org.cangnova.cangjie.LanguageVersionSettings,
    ): Boolean

    public companion object {
        /** 创建一个只允许给定种类集合在指定位置出现的谓词。 */
        public fun memberOf(vararg kinds: DeclarationKind): ModifierTargetPredicate =
            ModifierTargetPredicate { target, _ ->
                target.site == Site.MEMBER && target.kind in kinds
            }

        /** 创建一个只允许给定种类集合在指定容器种类的成员位置出现的谓词（容器细分）。 */
        public fun memberOfIn(
            containers: Set<DeclarationKind>,
            vararg kinds: DeclarationKind,
        ): ModifierTargetPredicate =
            ModifierTargetPredicate { target, _ ->
                target.site == Site.MEMBER && target.kind in kinds && target.container in containers
            }

        /** 创建一个只允许给定种类集合作为声明头出现的谓词。 */
        public fun headOf(vararg kinds: DeclarationKind): ModifierTargetPredicate =
            ModifierTargetPredicate { target, _ ->
                target.site == Site.HEAD && target.kind in kinds
            }

        /** 创建一个只允许给定种类集合作为局部声明出现的谓词。 */
        public fun localOf(vararg kinds: DeclarationKind): ModifierTargetPredicate =
            ModifierTargetPredicate { target, _ ->
                target.site == Site.LOCAL && target.kind in kinds
            }

        /** 创建一个允许给定种类集合在任意位置出现的谓词。 */
        public fun anySiteOf(vararg kinds: DeclarationKind): ModifierTargetPredicate =
            ModifierTargetPredicate { target, _ ->
                target.kind in kinds
            }

        /** 创建一个允许任意目标的谓词（无约束）。 */
        public fun any(): ModifierTargetPredicate = ModifierTargetPredicate { _, _ -> true }

        /** 组合多个谓词为"任一命中即允许"。 */
        public fun anyOf(vararg predicates: ModifierTargetPredicate): ModifierTargetPredicate =
            ModifierTargetPredicate { target, settings ->
                predicates.any { it.isAllowed(target, settings) }
            }
    }
}

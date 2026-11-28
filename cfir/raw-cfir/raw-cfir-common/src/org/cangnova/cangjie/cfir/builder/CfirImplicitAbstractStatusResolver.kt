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

package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.CjSourceKind

/** Callable 的语法种类；隐式 abstract 规则对函数和属性有不同的 body 判定。 */
enum class CfirImplicitAbstractMemberKind {
    FUNCTION,
    PROPERTY,
}

/** Callable 所处的直接 class-like 容器种类。 */
enum class CfirImplicitAbstractContainerKind {
    CLASS,
    INTERFACE,
    OTHER,
}

/**
 * 两种 raw CFIR builder 共同使用的隐式 abstract 输入事实。
 *
 * PSI 与 LightTree 只负责读取各自语法树上的事实；语言规则集中在
 * [CfirImplicitAbstractStatusResolver]，避免两条构建路径各自解释类成员的模态。
 */
data class CfirImplicitAbstractMemberFacts(
    val memberKind: CfirImplicitAbstractMemberKind,
    val containerKind: CfirImplicitAbstractContainerKind,
    val sourceKind: CjSourceKind,
    val isForeign: Boolean,
    val hasIntrinsicAnnotation: Boolean,
    val isCommon: Boolean,
    val isExplicitlyAbstract: Boolean,
    val containerIsCommon: Boolean,
    val containerIsSpecific: Boolean,
    val containerIsAbstract: Boolean,
    val hasBody: Boolean,
    val hasExplicitAccessors: Boolean,
)

/**
 * 解析 class-like callable 的 parser-derived abstract 状态。
 *
 * 规则对齐官方 `CanBeAbstract` 与 class member body abstractness：声明文件只抑制函数推断；
 * common 成员不隐式 abstract；正常源码中 common/specific 抽象类的无修饰成员不隐式 abstract。
 * `.cj.d` 的属性状态保持既有解析结果，因为当前 v1.1.3 证据只验证了普通源码。
 */
object CfirImplicitAbstractStatusResolver {
    /** 返回当前 callable 是否因无实现语法而隐式 abstract。 */
    fun isImplicitAbstract(facts: CfirImplicitAbstractMemberFacts): Boolean {
        if (facts.containerKind != CfirImplicitAbstractContainerKind.CLASS &&
            facts.containerKind != CfirImplicitAbstractContainerKind.INTERFACE
        ) return false
        if (facts.isForeign || facts.hasIntrinsicAnnotation) return false
        if (facts.memberKind == CfirImplicitAbstractMemberKind.FUNCTION && facts.sourceKind.isDeclaration) return false

        if (facts.containerKind == CfirImplicitAbstractContainerKind.CLASS) {
            if (facts.isCommon) return false
            val isAbstractCjmpClass = facts.containerIsAbstract &&
                    (facts.containerIsCommon || facts.containerIsSpecific)
            if (isAbstractCjmpClass && !facts.isExplicitlyAbstract &&
                !(facts.memberKind == CfirImplicitAbstractMemberKind.PROPERTY && facts.sourceKind.isDeclaration)
            ) return false
        }

        return when (facts.memberKind) {
            CfirImplicitAbstractMemberKind.FUNCTION -> !facts.hasBody
            CfirImplicitAbstractMemberKind.PROPERTY -> !facts.hasBody && !facts.hasExplicitAccessors
        }
    }
}

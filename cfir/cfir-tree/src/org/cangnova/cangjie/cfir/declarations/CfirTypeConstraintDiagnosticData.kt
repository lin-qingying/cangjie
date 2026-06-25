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
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 泛型 where 约束相关的诊断定位数据。
 *
 * CFIR 类型系统只需要解析后的约束关系，但 checker 还需要保留源码约束节点位置，
 * 以便报告“约束对象不是类型参数”等官方语义诊断。
 *
 * @property typeConstraints 声明上的 where 约束源码引用列表。
 */
data class CfirTypeConstraintDiagnosticData(
    /**
     * 声明上的 where 约束源码引用列表。
     */
    val typeConstraints: List<CfirTypeConstraintReference>,
) {
    /**
     * 类型约束诊断数据的空值对象。
     */
    companion object {
        /**
         * 不包含任何 where 约束引用的诊断数据。
         */
        val EMPTY = CfirTypeConstraintDiagnosticData(
            typeConstraints = emptyList(),
        )
    }
}

/**
 * 单条 where 约束在源码中的定位引用。
 *
 * @property parameterName 约束左侧声明的类型参数名。
 * @property source 约束中的类型参数名位置，用于“不是类型参数名”一类诊断。
 * @property constraintSource 整条 where 约束位置，对齐官方 generic constraint 诊断节点。
 */
data class CfirTypeConstraintReference(
    /**
     * 约束左侧声明的类型参数名。
     */
    val parameterName: Name,
    /**
     * 约束中的类型参数名位置，用于“不是类型参数名”一类诊断。
     */
    val source: CjSourceElement,
    /**
     * 整条 where 约束位置，对齐官方 generic constraint 诊断节点。
     */
    val constraintSource: CjSourceElement = source,
)

/**
 * [CfirTypeConstraintDiagnosticData] 在声明属性表中的键。
 */
private object TypeConstraintDiagnosticDataKey : CfirDeclarationDataKey()

/**
 * 声明属性上的泛型 where 约束诊断数据。
 *
 * raw CFIR 构建阶段写入该属性，泛型约束 checker 使用它还原源码级诊断位置。
 */
var CfirDeclarationAttributes.typeConstraintDiagnosticData: CfirTypeConstraintDiagnosticData? by CfirDeclarationDataRegistry.attributesAccessor(
    TypeConstraintDiagnosticDataKey
)

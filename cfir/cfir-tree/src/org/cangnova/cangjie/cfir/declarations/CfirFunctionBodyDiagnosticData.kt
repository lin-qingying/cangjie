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
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 函数体级源码结构的诊断数据。
 *
 * 仓颉官方 AST 的 FuncBody 会保留多个参数列表，用于 constructor/finalizer/getter/setter
 * 这类声明的柯里化非法诊断。CFIR 语义模型仍只使用第一个参数列表作为真实签名，
 * 这里仅保存源码参数列表位置，供 checker 按官方语义报告诊断。
 */
data class CfirFunctionBodyDiagnosticData(
    /**
     * 源码中出现的函数体参数列表引用。
     *
     * 第一个列表通常对应真实 CFIR 签名，其余列表仅用于非法柯里化形态的诊断定位。
     */
    val valueParameterLists: List<CfirValueParameterListReference>,
) {
    /**
     * 函数体诊断数据的空值对象。
     */
    companion object {
        /**
         * 不包含任何额外参数列表位置的诊断数据。
         */
        val EMPTY = CfirFunctionBodyDiagnosticData(
            valueParameterLists = emptyList(),
        )
    }
}

/**
 * 函数体参数列表在源码中的位置引用。
 *
 * @property source 参数列表整体的源码元素，用于把非法函数体形态诊断报告到官方语义要求的位置。
 */
data class CfirValueParameterListReference(
    val source: CjSourceElement,
)

/**
 * [CfirFunctionBodyDiagnosticData] 在声明属性表中的键。
 */
private object FunctionBodyDiagnosticDataKey : CfirDeclarationDataKey()

/**
 * 声明属性上的函数体诊断数据。
 *
 * 该属性由 raw CFIR 构建阶段写入，checker 在函数体语义检查时读取。
 */
var CfirDeclarationAttributes.functionBodyDiagnosticData: CfirFunctionBodyDiagnosticData? by CfirDeclarationDataRegistry.attributesAccessor(
    FunctionBodyDiagnosticDataKey
)

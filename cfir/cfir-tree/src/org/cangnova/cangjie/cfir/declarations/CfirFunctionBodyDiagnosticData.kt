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
    val valueParameterLists: List<CfirValueParameterListReference>,
) {
    companion object {
        val EMPTY = CfirFunctionBodyDiagnosticData(
            valueParameterLists = emptyList(),
        )
    }
}

data class CfirValueParameterListReference(
    val source: CjSourceElement,
)

private object FunctionBodyDiagnosticDataKey : CfirDeclarationDataKey()

var CfirDeclarationAttributes.functionBodyDiagnosticData: CfirFunctionBodyDiagnosticData? by CfirDeclarationDataRegistry.attributesAccessor(
    FunctionBodyDiagnosticDataKey
)

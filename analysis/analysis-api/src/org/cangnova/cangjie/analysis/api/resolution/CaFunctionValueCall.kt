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

package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

/**
 * 通过函数类型变量调用 `invoke` 的解析结果。
 *
 * 函数值调用的解析目标是变量符号，而非普通函数符号，因此它不能伪装成
 * [CaFunctionCall]。该模型保留实际被调用的变量和其函数类型。
 */
interface CaFunctionValueCall : CaCall {
    /** 函数值引用解析到的变量、字段或属性。 */
    val calleeSymbol: CaVariableSymbol

    /** 调用点所使用的函数类型。 */
    val functionType: CaFunctionType
}

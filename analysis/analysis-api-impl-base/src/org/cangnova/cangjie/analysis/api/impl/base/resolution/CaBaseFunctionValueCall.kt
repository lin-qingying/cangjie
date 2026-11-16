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

package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionValueCall
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

/** Analysis API 函数值调用的基础实现。 */
@CaImplementationDetail
class CaBaseFunctionValueCall(
    private val backingCalleeSymbol: CaVariableSymbol,
    private val backingFunctionType: CaFunctionType,
) : CaFunctionValueCall {
    override val token: CaLifetimeToken
        get() = backingCalleeSymbol.token

    override val calleeSymbol: CaVariableSymbol
        get() = withValidityAssertion { backingCalleeSymbol }

    override val functionType: CaFunctionType
        get() = withValidityAssertion { backingFunctionType }
}

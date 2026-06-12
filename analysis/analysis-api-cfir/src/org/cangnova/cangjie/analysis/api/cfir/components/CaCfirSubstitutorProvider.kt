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

package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirMapBackedSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.components.CaSubstitutorProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 对齐 Kotlin `KaFirSubstitutorProvider` 的组件边界，
 * 统一提供 Analysis API 可见的公开替换器构造入口。
 */
internal class CaCfirSubstitutorProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSubstitutorProvider {
    override fun createSubstitutor(mappings: Map<CaTypeParameterSymbol, CaType>): CaSubstitutor = withValidityAssertion {
        if (mappings.isEmpty()) return CaSubstitutor.Empty(token)

        val cfirMappings = buildMap<TypeConstructorMarker, org.cangnova.cangjie.cfir.types.ConeCangJieType> {
            mappings.forEach { (typeParameterSymbol, type) ->
                val cfirSymbol = (typeParameterSymbol as? CaCfirSymbol<*>)?.cfirSymbol as? CfirTypeParameterSymbol
                    ?: error("Only CFIR type parameter symbols can be used to build a CFIR substitutor")
                val cfirType = (type as? CaCfirType)?.coneType
                    ?: error("Only CFIR types can be used to build a CFIR substitutor")
                put(cfirSymbol.toLookupTag(), cfirType)
            }
        }

        val coneSubstitutor = CfirTypeSubstitutorByMap(cfirMappings)
        return CaCfirMapBackedSubstitutor(
            mappings = mappings.entries.map { (symbol, type) -> symbol to type },
            substitutor = coneSubstitutor,
            builder = analysisSession.cfirSymbolBuilder,
        )
    }
}

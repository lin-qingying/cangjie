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

package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor

/**
 * 对齐 Kotlin `KaFirBasePropertyAccessorSymbol` 的访问器共性层。
 *
 * 仓颉 getter/setter 都依附于 property 本体，因此这里统一承接：
 * 1. property -> accessor 的 PSI / CFIR 映射；
 * 2. 访问器公共状态、可见性、返回类型与参数恢复；
 * 3. setter 参数优先使用 accessor PSI，对齐 Kotlin 的 parameterImpl 策略。
 */
internal sealed interface CaCfirBasePropertyAccessorSymbol :
    CaCfirCjBasedSymbol<CjPropertyAccessor, CfirPropertyAccessorSymbol> {
    /**
     * 访问器所属的公开属性符号。
     */
    val owningCaProperty: CaPropertySymbol

    /**
     * 访问器所属的 CFIR 属性符号实现。
     */
    private val owningCfirProperty: CaCfirPropertySymbol
        get() = owningCaProperty as? CaCfirPropertySymbol
            ?: error("Property accessor owner must be CaCfirPropertySymbol: ${owningCaProperty::class.simpleName}")

    /**
     * 当前访问器是否是 getter。
     */
    private val isGetterAccessor: Boolean
        get() = this is CaCfirBasePropertyGetterSymbol

    /**
     * 属性访问器从所属 property 动态取得 CFIR 符号，因此不提供独立 lazy 符号。
     */
    override val lazyCfirSymbol: Lazy<CfirPropertyAccessorSymbol>
        get() = throw UnsupportedOperationException()

    /**
     * 从所属属性 PSI 中取得 getter 或 setter PSI。
     */
    override val backingPsi: CjPropertyAccessor?
        get() {
            val property = owningCfirProperty.backingPsi as? CjProperty ?: return null
            return if (isGetterAccessor) property.getter else property.setter
        }

    /**
     * 从所属 CFIR property 中取得 getter 或 setter 符号。
     */
    override val cfirSymbol: CfirPropertyAccessorSymbol
        get() {
            val propertySymbol = owningCfirProperty.cfirSymbol
            return if (isGetterAccessor) {
                propertySymbol.getterSymbol
            } else {
                propertySymbol.setterSymbol
            } ?: error("${if (isGetterAccessor) "Getter" else "Setter"} accessor is missing")
        }

    /**
     * 访问器复用所属属性绑定的 Analysis session。
     */
    override val analysisSession: CaCfirSession
        get() = owningCfirProperty.analysisSession

    /**
     * 访问器所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 访问器 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * 访问器公开注解列表实现。
     */
    val annotationsImpl: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder) }

    /**
     * 访问器公开 PSI。
     */
    val psiImpl: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * 访问器来源与所属属性保持一致。
     */
    val originImpl
        get() = withValidityAssertion { owningCaProperty.origin }

    /**
     * 属性访问器当前不暴露独立 callableId。
     */
    val callableIdImpl: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion { null }

    /**
     * 访问器显式 receiver 类型。
     */
    val receiverTypeImpl: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    /**
     * 访问器返回类型。
     */
    val returnTypeImpl: CaType
        get() = withValidityAssertion { cfirSymbol.returnType(builder) }

    /**
     * 访问器在公开符号位置上归类为属性位置。
     */
    val locationImpl: CaSymbolLocation
        get() = CaSymbolLocation.PROPERTY

    /**
     * 访问器公开可见性。
     */
    val visibilityImpl: CaSymbolVisibility
        get() = status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    /**
     * 访问器可见性是否显式写出。
     */
    val isVisibilityExplicitImpl: Boolean
        get() = status?.isVisibilityExplicit == true

    /**
     * 访问器 modality。
     */
    val modalityImpl: CaSymbolModality?
        get() = status?.modality?.asPublicModality()

    /**
     * 访问器 modality 是否显式写出。
     */
    val isModalityExplicitImpl: Boolean
        get() = status?.isModalityExplicit == true

    /**
     * 访问器是否为 static。
     */
    val isStaticImpl: Boolean
        get() = status?.isStatic == true

    /**
     * 访问器是否为 const。
     */
    val isConstImpl: Boolean
        get() = status?.isConst == true

    /**
     * 访问器是否为 mutating。
     */
    val isMutatingImpl: Boolean
        get() = status?.isMut == true

    /**
     * 访问器是否覆盖父级声明。
     */
    val isOverrideImpl: Boolean
        get() = status?.isOverride == true

    /**
     * 访问器是否带 operator 语义。
     */
    val isOperatorImpl: Boolean
        get() = status?.isOperator == true

    /**
     * 访问器是否带 unsafe 标记。
     */
    val isUnsafeImpl: Boolean
        get() = status?.isUnsafe == true

    /**
     * 访问器是否为 foreign 声明。
     */
    val isForeignImpl: Boolean
        get() = status?.isForeign == true

    /**
     * 访问器声明的类型参数列表。
     */
    val typeParametersImpl: List<CaTypeParameterSymbol>
        get() = (cfirSymbol.cfir as? CfirCallableDeclaration)
            ?.typeParameters
            ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    /**
     * 访问器声明的值参数列表。
     */
    val valueParametersImpl: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.valueParameters?.map { parameter -> parameter.symbol as CaValueParameterSymbol }
            } ?: (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.mapIndexed { parameterIndex, parameter ->
                    builder.variableBuilder.buildOwnedValueParameterSymbol(this@CaCfirBasePropertyAccessorSymbol as org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol, parameter, parameterIndex)
                }
                .orEmpty()
        }

    /**
     * 公开 API 看到的所属属性。
     */
    val owningPropertyImpl: CaPropertySymbol
        get() = owningCaProperty

    /**
     * 访问器是否为默认生成实现。
     */
    val isDefaultImpl: Boolean
        get() = status?.isDefault == true
}

/**
 * CFIR 属性 getter 符号共性接口。
 */
internal interface CaCfirBasePropertyGetterSymbol : CaCfirBasePropertyAccessorSymbol {
    /**
     * 创建可恢复当前 getter 符号的 pointer。
     */
    fun createGetterPointer(): org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaPropertyGetterSymbol> { psi ->
            (psi as? CjPropertyAccessor)?.symbol as? CaPropertyGetterSymbol
        } ?: org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertyGetterSymbolPointer(owningCaProperty.createPointer())
    }
}

/**
 * CFIR 属性 setter 符号共性接口。
 */
internal interface CaCfirBasePropertySetterSymbol : CaCfirBasePropertyAccessorSymbol {
    /**
     * setter 的唯一值参数。
     */
    val parameterImpl: CaValueParameterSymbol
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.valueParameters?.firstOrNull()?.symbol as? CaValueParameterSymbol
            } ?: valueParametersImpl.single()
        }

    /**
     * 创建可恢复当前 setter 符号的 pointer。
     */
    fun createSetterPointer(): org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaPropertySetterSymbol> { psi ->
            (psi as? CjPropertyAccessor)?.symbol as? CaPropertySetterSymbol
        } ?: org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertySetterSymbolPointer(owningCaProperty.createPointer())
    }
}

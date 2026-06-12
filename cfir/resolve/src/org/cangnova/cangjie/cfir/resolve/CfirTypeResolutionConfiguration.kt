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

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * `This` 类型解析语境。
 *
 * 仓颉官方编译器即使在 static、extend 或嵌套函数等非法位置报告
 * `This` 诊断，也会保留足够的 ClassThisTy/接收者类型信息供后续调用结果绑定，
 * 避免把一个非法 `This` 级联成无关的成员找不到或类型推断失败。
 */
data class ThisTypeResolutionContext(
    val type: ConeCangJieType,
    val isAllowed: Boolean,
    val disallowedDiagnosticKind: DiagnosticKind = DiagnosticKind.ThisTypeNotAllowed,
) {
    fun asDisallowed(): ThisTypeResolutionContext =
        if (!isAllowed) this else copy(isAllowed = false)
}

/**
 * 对齐 Kotlin `TypeResolutionConfiguration` 的类型解析配置。
 */
data class TypeResolutionConfiguration(
    /**
     * 类型解析作用域按“高优先级在前、低优先级在后”排列。
     *
     * `CfirTypeResolver` 会按当前迭代顺序直接命中第一个可见分类器，
     * 因此这里绝不能再把文件级/包级/导入级 scope 反转后传入，
     * 否则默认导入会抢在当前文件声明之前被解析。
     */
    val scopes: Iterable<CfirScope> = emptyList(),
    val containingClassDeclarations: List<CfirClass> = emptyList(),
    val thisTypeContext: ThisTypeResolutionContext? = null,
    val useSiteFile: CfirFile?,
    val topContainer: CfirDeclaration? = null,
    val scopeTypeParameters: Map<String, CfirTypeParameter> = emptyMap(),
) {
    fun withUseSiteFile(file: CfirFile): TypeResolutionConfiguration {
        if (useSiteFile === file) return this
        return copy(useSiteFile = file)
    }

    fun withTopContainer(container: CfirDeclaration?): TypeResolutionConfiguration {
        if (topContainer === container) return this
        return copy(topContainer = container)
    }

    fun withAdditionalTypeParameters(parameters: List<CfirTypeParameter>): TypeResolutionConfiguration {
        if (parameters.isEmpty()) return this
        val updated = LinkedHashMap(scopeTypeParameters)
        for (parameter in parameters) {
            updated[parameter.name.asString()] = parameter
        }
        return copy(scopeTypeParameters = updated)
    }

    fun withContainingClassDeclarations(classes: List<CfirClass>): TypeResolutionConfiguration {
        if (containingClassDeclarations === classes) return this
        return copy(containingClassDeclarations = classes)
    }

    fun withThisTypeContext(context: ThisTypeResolutionContext?): TypeResolutionConfiguration {
        if (thisTypeContext == context) return this
        return copy(thisTypeContext = context)
    }

    fun withScopes(scopes: Iterable<CfirScope>): TypeResolutionConfiguration {
        if (this.scopes === scopes) return this
        return copy(scopes = scopes)
    }

    companion object {
        val EMPTY: TypeResolutionConfiguration = TypeResolutionConfiguration(useSiteFile = null)
    }
}

typealias CfirTypeResolutionConfiguration = TypeResolutionConfiguration

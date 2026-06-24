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
    /**
     * 当前 `This` 可绑定到的接收者类型。
     */
    val type: ConeCangJieType,
    /**
     * 当前语法位置是否允许直接使用 `This` 类型。
     */
    val isAllowed: Boolean,
    /**
     * 禁止使用 `This` 时应报告的诊断种类。
     */
    val disallowedDiagnosticKind: DiagnosticKind = DiagnosticKind.ThisTypeNotAllowed,
) {
    /**
     * 复用当前接收者类型创建“不允许使用”的语境。
     *
     * 解析器仍保留 [type] 以便后续成员绑定继续工作，诊断阶段通过 [isAllowed] 决定是否报告。
     */
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
    /**
     * 当前类型位置外层包含的类声明，由内到外排列。
     *
     * 类型解析需要该链路识别嵌套类、`This` 语境以及类级类型参数的可见范围。
     */
    val containingClassDeclarations: List<CfirClass> = emptyList(),
    /**
     * 当前可用的 `This` 类型语境；为 `null` 表示当前位置没有可绑定的类型接收者。
     */
    val thisTypeContext: ThisTypeResolutionContext? = null,
    /**
     * 类型引用所在的使用点文件。
     *
     * 导入、可见性和同包规则都依赖使用点文件；合成解析场景允许为空。
     */
    val useSiteFile: CfirFile?,
    /**
     * 当前类型引用所在的顶层容器声明。
     *
     * 该信息用于区分文件级、类级和局部声明下的类型解析边界。
     */
    val topContainer: CfirDeclaration? = null,
    /**
     * 当前作用域额外暴露的类型参数索引，键为源码名称。
     */
    val scopeTypeParameters: Map<String, CfirTypeParameter> = emptyMap(),
) {
    /**
     * 切换类型解析的使用点文件。
     */
    fun withUseSiteFile(file: CfirFile): TypeResolutionConfiguration {
        if (useSiteFile === file) return this
        return copy(useSiteFile = file)
    }

    /**
     * 切换当前类型引用所在的顶层容器声明。
     */
    fun withTopContainer(container: CfirDeclaration?): TypeResolutionConfiguration {
        if (topContainer === container) return this
        return copy(topContainer = container)
    }

    /**
     * 在现有类型参数索引之后追加新的类型参数声明。
     *
     * 后加入的同名参数会覆盖旧值，用于模拟内层作用域遮蔽外层同名类型参数的语义。
     */
    fun withAdditionalTypeParameters(parameters: List<CfirTypeParameter>): TypeResolutionConfiguration {
        if (parameters.isEmpty()) return this
        val updated = LinkedHashMap(scopeTypeParameters)
        for (parameter in parameters) {
            updated[parameter.name.asString()] = parameter
        }
        return copy(scopeTypeParameters = updated)
    }

    /**
     * 替换当前类型位置的外层类声明链。
     */
    fun withContainingClassDeclarations(classes: List<CfirClass>): TypeResolutionConfiguration {
        if (containingClassDeclarations === classes) return this
        return copy(containingClassDeclarations = classes)
    }

    /**
     * 替换当前 `This` 类型解析语境。
     */
    fun withThisTypeContext(context: ThisTypeResolutionContext?): TypeResolutionConfiguration {
        if (thisTypeContext == context) return this
        return copy(thisTypeContext = context)
    }

    /**
     * 替换类型解析作用域序列。
     */
    fun withScopes(scopes: Iterable<CfirScope>): TypeResolutionConfiguration {
        if (this.scopes === scopes) return this
        return copy(scopes = scopes)
    }

    /**
     * 类型解析配置的共享空实例。
     */
    companion object {
        /**
         * 不携带作用域、使用点和类型参数的空配置。
         */
        val EMPTY: TypeResolutionConfiguration = TypeResolutionConfiguration(useSiteFile = null)
    }
}

/**
 * CFIR 层保留的类型解析配置别名。
 *
 * 新代码应直接使用 [TypeResolutionConfiguration]；别名用于兼容既有 CFIR 命名和渐进迁移。
 */
typealias CfirTypeResolutionConfiguration = TypeResolutionConfiguration

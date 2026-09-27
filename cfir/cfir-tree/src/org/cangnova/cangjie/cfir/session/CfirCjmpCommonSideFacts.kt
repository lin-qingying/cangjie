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
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.primaryBindingNameOrNull
import org.cangnova.cangjie.name.Name

/**
 * CJMP common 方向判定事实（cjc 1.1.3 `MatchCJMPDecls` 的 common 声明循环）。
 *
 * 源码 common（测试多模块、IDE）由检查器消费；反序列化 common（CLI specific 编译加载 cjo）由装配层消费——
 * 两条路径共用同一判据，避免分叉。
 */
object CfirCjmpCommonSideFacts {
    /**
     * common 声明及其 common 成员（nominal 与 extend 成员；common enum 的全部构造器——官方构造器随外层携带
     * COMMON），附外层声明。
     */
    fun commonDeclarationsWithOuter(
        topLevel: Collection<CfirDeclaration>,
    ): List<Pair<CfirDeclaration, CfirDeclaration?>> = buildList {
        for (common in topLevel) {
            if ((common as? CfirMemberDeclaration)?.status?.isCommon != true) continue
            add(common to null)
            val members = when (common) {
                is CfirClassLikeDeclaration -> common.declarations
                is CfirExtend -> common.declarations
                else -> emptyList()
            }
            for (member in members) {
                if (member is CfirEnumConstructor) {
                    add(member to common)
                    continue
                }
                val status = (member as? CfirMemberDeclaration)?.status ?: continue
                if (!status.isCommon) continue
                add(member to common)
            }
        }
    }

    /**
     * 该 common 声明是否因未被 specific 实现而须报 `NOT_MATCHED`（官方 `MustMatchWithSpecific` +
     * `specificImplementation` 判定；第二绑定由 [hasMultipleImplementations] 单独报告）。
     */
    fun mustReportNotMatched(
        common: CfirDeclaration,
        outer: CfirDeclaration?,
        storage: CfirCjmpMappingStorage,
    ): Boolean {
        if (storage.specificBindingsFor(common).isNotEmpty()) return false
        if (common is CfirEnumConstructor) {
            // 无 specific enum 实现时，官方对 COMMON_WITH_DEFAULT 外层 enum 豁免其构造器配对。
            return !(outer != null && outer.cjmpHasCommonDefault() && storage.specificBindingsFor(outer).isEmpty())
        }
        if (common is CfirPatternVariable &&
            common.pattern.bindingVariables().any { storage.specificBindingsFor(it).isNotEmpty() }
        ) return false
        if (common.cjmpHasCommonDefault()) return false
        if (outer is CfirInterface) return false
        return true
    }

    /** common 声明被多个 specific 绑定（官方 `TrySetSpecificImpl` 的 `sema_multiple_common_implementations`）。 */
    fun hasMultipleImplementations(common: CfirDeclaration, storage: CfirCjmpMappingStorage): Boolean =
        storage.specificBindingsFor(common).size > 1

    /** 官方 `DeclKindToString`（TypeCheckUtil.cpp `DECL2STRMAP`）。 */
    fun declKind(declaration: CfirDeclaration): String = when (declaration) {
        is CfirClass -> "class"
        is CfirStruct -> "struct"
        is CfirInterface -> "interface"
        is CfirEnum -> "enum"
        is CfirExtend -> "extend"
        is CfirProperty -> "property"
        is CfirFunction -> "function"
        is CfirVariable -> "variable"
        is CfirTypeAlias -> "type alias"
        else -> "declaration"
    }

    /** 声明名（官方 `decl.identifier`）。 */
    fun declName(declaration: CfirDeclaration): Name = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.name
        is CfirEnumConstructor -> declaration.name
        is CfirExtend -> (declaration.extendedTypeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)
            ?.coneType?.let { type ->
                (type as? org.cangnova.cangjie.cfir.types.ConeClassLikeType)?.classId?.shortClassName
                    ?: Name.identifier(type.toString())
            } ?: Name.special("<extend>")
        is CfirPatternVariable -> declaration.pattern.primaryBindingNameOrNull() ?: declaration.symbol.name
        is CfirCallableDeclaration -> declaration.symbol.name
        else -> Name.special("<no name>")
    }

    /**
     * 官方 `DiagNotMatchedDecl` 的第二参数："constructor"、"enum 'E' constructor 'C'" 或 "Kind 'name'"。
     *
     * @param outer enum 构造器所在 enum（其余声明不需要）。
     */
    fun declInfo(declaration: CfirDeclaration, outer: CfirDeclaration? = null): String {
        if (declaration is CfirConstructor) return "constructor"
        if (declaration is CfirEnumConstructor) {
            val enumName = outer?.let(::declName)?.asString()
                ?: declaration.symbol.callableId.classId?.shortClassName?.asString().orEmpty()
            return "enum '$enumName' constructor '${declaration.name.asString()}'"
        }
        return "${declKind(declaration)} '${declName(declaration).asString()}'"
    }

    /**
     * 官方 `TrySetSpecificImpl` 的 kind 实参：函数为 "function"，属性为 "property <name>"，其余按 [declKind]。
     */
    fun implementationKind(declaration: CfirDeclaration): String = when (declaration) {
        is CfirProperty -> "property ${declName(declaration).asString()}"
        is CfirFunction -> "function"
        else -> declKind(declaration)
    }
}

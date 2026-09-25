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

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.patterns.bindingVariables

/**
 * CJMP common 候选查找（对齐 Kotlin `FirExpectActualResolver.findExpectForActual`）。
 *
 * 架构要点（G3 修复）：
 * - 一律经 **依赖侧** 符号提供器（[CfirSession.dependenciesSymbolProvider]）查找；
 *   绝不会命中 specific 模块自身的同名声明（普通查询"本模块优先"的顺序在此不适用）；
 * - 候选过滤：`status.isCommon && moduleData ∈ 传递 dependsOn 依赖`（DAG 语义，D3：官方多 parent 链）；
 * - 模块同一性：`LLCfirModuleData` 每次访问重建实例，用 `name` 做等价判据
 *   （name 由 caModule.moduleDescription 派生，模块内唯一；Kotlin 用 FirModuleData 身份比较，
 *   名称唯一前提下语义等价）。
 *
 * 成员查找对位 Kotlin `expectContainingClass.getCallablesForExpectClass(name)`：
 * 先经 ClassId 找到 common 容器，再在容器成员中按名筛选。
 */
object CfirCjmpResolver {
    /** extend 配对键：扩展目标类型 + 实现接口集合（渲染后的 cone 类型文本，跨 session 按结构比较）。 */
    private fun extendKey(extend: org.cangnova.cangjie.cfir.declarations.CfirExtend): Pair<String, Set<String>>? {
        val target = extend.extendedTypeRef.coneTypeOrNullSafe()?.toString() ?: return null
        val interfaces = extend.superTypeRefs.mapNotNull { it.coneTypeOrNullSafe()?.toString() }.toSet()
        return target to interfaces
    }

    private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.coneTypeOrNullSafe(): org.cangnova.cangjie.cfir.types.ConeCangJieType? =
        runCatching { (this as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)?.coneType }.getOrNull()

    /**
     * 求 [specific] 声明的 common 候选（结构匹配前的候选集合，未做兼容性判定）。
     */
    fun findCommonCandidates(specific: CfirDeclaration, session: CfirSession): List<CfirDeclaration> {
        val dependencyModules = specific.moduleData.allRefinementDependencies
        if (dependencyModules.isEmpty()) return emptyList()
        val dependencyNames = dependencyModules.mapTo(HashSet()) { it.name }
        val provider = session.dependenciesSymbolProvider

        val rawCandidates: List<CfirDeclaration> = when (specific) {
            is CfirClassLikeDeclaration ->
                provider.getClassLikeSymbolByClassId(specific.symbol.classId)
                    ?.cfir
                    ?.let(::listOf)
                    .orEmpty()

            is org.cangnova.cangjie.cfir.declarations.CfirExtend -> {
                // 官方 `MergeCJMPExtensions` 键：扩展类型 + `<:` 接口集；候选取 refinement 依赖中的 common extend
                val key = extendKey(specific) ?: return emptyList()
                session.extendProvider.getAllExtends().filter { candidate ->
                    candidate !== specific && extendKey(candidate) == key
                }
            }

            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> {
                // 模式变量以绑定名参与顶层查找（`let v: T = e` / 元组模式逐变量），对位官方 VarDecl / VarWithPatternDecl
                val packageName = specific.symbol.callableId.packageName
                specific.pattern.bindingVariables().flatMap { binding ->
                    provider.getTopLevelCallableSymbols(packageName, binding.name).map { it.cfir }
                }
            }

            is CfirCallableDeclaration -> {
                val callableId = specific.symbol.callableId
                val containingClassId = callableId.classId
                if (containingClassId == null) {
                    provider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
                        .map { it.cfir }
                } else {
                    val commonContainer = provider.getClassLikeSymbolByClassId(containingClassId)?.cfir
                    (commonContainer as? CfirClassLikeDeclaration)
                        ?.declarations
                        .orEmpty()
                        .filterIsInstance<CfirCallableDeclaration>()
                        .filter { it.symbol.name == specific.symbol.name }
                }
            }

            else -> emptyList()
        }

        return rawCandidates
            .distinct()
            .filter { candidate ->
                (candidate as? CfirMemberDeclaration)?.status?.isCommon == true &&
                        candidate.moduleData.name in dependencyNames
            }
    }
}

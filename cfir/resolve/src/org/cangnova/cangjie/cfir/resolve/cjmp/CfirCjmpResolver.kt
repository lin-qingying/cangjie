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
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol

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
 * 先从 specific 声明的真实 owner 取得已配对 common 容器，再按名筛选其成员。
 */
object CfirCjmpResolver {
    /**
     * 返回 specific callable 的 first-fit 匹配组。
     *
     * 顶层声明跨文件按官方文件名与源码位置稳定排序；class/extend 成员保持所属声明列表的源码顺序，
     * 以便 eager 遍历和 LL 对单个成员的并发请求得到相同的绑定胜者。
     */
    fun findSpecificCallablesInMatchOrder(
        specific: CfirCallableDeclaration,
        session: CfirSession,
    ): List<CfirCallableDeclaration> {
        if (!specific.status.isSpecific || specific is CfirEnumConstructor) return listOf(specific)

        val specificContainer = specific.getContainingCjmpDeclaration()
        val declarations = when (specificContainer) {
            is CfirClassLikeDeclaration -> specificContainer.declarations
            is CfirExtend -> specificContainer.declarations
            else -> null
        }
        if (declarations != null) {
            val matchingMembers = declarations
                .filterIsInstance<CfirCallableDeclaration>()
                .filter { candidate ->
                    candidate.status.isSpecific && candidate.symbol.name == specific.symbol.name
                }
            return (matchingMembers + specific).distinctBy { it.symbol }
        }
        return findSpecificTopLevelCallablesInMatchOrder(specific, session)
    }

    /**
     * 返回顶层 specific callable 的完整同名匹配组，并按官方文件名与声明顺序排列。
     *
     * LL 按目标懒解析时也必须先完成该组，才能保持 cjc `MatchCJMPDecls` 的 first-fit 绑定次序。
     */
    fun findSpecificTopLevelCallablesInMatchOrder(
        specific: CfirCallableDeclaration,
        session: CfirSession,
    ): List<CfirCallableDeclaration> {
        if (!specific.status.isSpecific) return listOf(specific)
        val callableId = specific.symbol.callableId
        if (callableId.classId != null || specific.isLocal || session.extendProvider.getContainingExtend(specific.symbol) != null) {
            return listOf(specific)
        }

        val siblings = session.symbolProvider
            .getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
            .asSequence()
            .map { it.cfir }
            .filterIsInstance<CfirCallableDeclaration>()
            .filter { candidate ->
                candidate.moduleData.name == specific.moduleData.name &&
                        candidate.status.isSpecific &&
                        candidate.symbol.callableId == callableId &&
                        !candidate.isLocal &&
                        session.extendProvider.getContainingExtend(candidate.symbol) == null
            }
            .toList()
        return (listOf(specific) + siblings)
            .distinctBy { it.symbol }
            .sortedWith(
                compareBy<CfirCallableDeclaration>(
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.name.orEmpty() },
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.path.orEmpty() },
                    { it.source?.startOffset ?: Int.MAX_VALUE },
                    { it.source?.endOffset ?: Int.MAX_VALUE },
                ),
            )
    }

    /** 同名 pattern binding 也共享官方 specific 声明 first-fit 顺序。 */
    fun findSpecificTopLevelPatternVariablesInMatchOrder(
        specific: CfirPatternVariable,
        session: CfirSession,
    ): List<CfirPatternVariable> {
        if (!specific.status.isSpecific || specific.symbol.callableId.classId != null || specific.isLocal ||
            session.extendProvider.getContainingExtend(specific.symbol) != null
        ) {
            return listOf(specific)
        }

        val bindingNames = specific.pattern.bindingVariables().mapTo(linkedSetOf()) { it.name }
        val candidates = bindingNames.flatMap { bindingName ->
            session.symbolProvider
                .getTopLevelCallableSymbols(specific.symbol.callableId.packageName, bindingName)
                .map { it.cfir }
                .filterIsInstance<CfirPatternVariable>()
        }.filter { candidate ->
            candidate.moduleData.name == specific.moduleData.name &&
                    candidate.status.isSpecific &&
                    candidate.symbol.callableId.classId == null &&
                    !candidate.isLocal &&
                    session.extendProvider.getContainingExtend(candidate.symbol) == null &&
                    candidate.pattern.bindingVariables().any { it.name in bindingNames }
        }

        return (listOf(specific) + candidates)
            .distinctBy { it.symbol }
            .sortedWith(
                compareBy<CfirPatternVariable>(
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.name.orEmpty() },
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.path.orEmpty() },
                    { it.source?.startOffset ?: Int.MAX_VALUE },
                    { it.source?.endOffset ?: Int.MAX_VALUE },
                ),
            )
    }

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
                // 官方 `MergeCJMPExtensions` 键：扩展类型、接口集及按位置映射的泛型约束。
                specific.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
                session.extendProvider.getAllExtends().filter { candidate ->
                    candidate.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
                    candidate !== specific &&
                            CfirCjmpMatcher.haveSameExtendKey(specific, candidate) == CjmpTypeCompatibility.COMPATIBLE
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
                check(callableId.classId == null) {
                    "Member common candidates must be queried through the matched containing declaration"
                }
                provider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
                    .map { it.cfir }
            }

            else -> emptyList()
        }

        return rawCandidates
            .distinct()
            .filter { candidate ->
                // 官方 `ParseCJMPDecl` 禁止 primary constructor 带 common/specific；它不能成为
                // specific secondary init 的 CJMP 对应物，即使 common CJO 将它放在 owner 成员表中。
                (candidate !is CfirConstructor || !candidate.isPrimary) &&
                        (candidate as? CfirMemberDeclaration)?.status?.isCommon == true &&
                        candidate.moduleData.name in dependencyNames
            }
    }

    /**
     * 在已配对的 nominal 或 extend 容器中查找成员候选。
     *
     * 该入口由成员自己的 CJMP_MATCHING 调用使用；common 容器必须来自 specific 父声明的已存储配对，
     * 以保证外层匹配失败时不会独立配对其子成员。
     */
    fun findCommonMemberCandidates(
        specific: CfirCallableDeclaration,
        commonContainer: CfirDeclaration,
    ): List<CfirCallableDeclaration> = findCommonMemberCandidates(specific, listOf(commonContainer))

    /** 同 key 的多个 common extend 都贡献其直接成员候选。 */
    fun findCommonMemberCandidates(
        specific: CfirCallableDeclaration,
        commonContainers: List<CfirDeclaration>,
    ): List<CfirCallableDeclaration> = commonContainers.flatMap { commonContainer ->
        val commonDeclarations = when (commonContainer) {
            is CfirClassLikeDeclaration -> commonContainer.declarations
            is CfirExtend -> commonContainer.declarations
            else -> emptyList()
        }
        commonDeclarations.filterIsInstance<CfirCallableDeclaration>().filter { candidate ->
            (candidate !is CfirConstructor || !candidate.isPrimary) &&
                    candidate.status.isCommon && candidate.symbol.name == specific.symbol.name
        }
    }.distinctBy { it.symbol }
}

/**
 * CJMP 成员唯一的结构归属入口。extend 是独立 owner，其余名义成员服从声明站点 provider；
 * common 候选只能从该 specific owner 的配对结果取得，不能用相同 ClassId 重查依赖侧容器。
 */
internal fun CfirCallableDeclaration.getContainingCjmpDeclaration(): CfirDeclaration? {
    val declarationSession = moduleData.session
    return declarationSession.extendProvider.getContainingExtend(symbol)
        ?: symbol.getContainingClassSymbol()?.cfir
}

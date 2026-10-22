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

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticCollectionStateKey
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.utils.SmartSet
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 冲突图中由相同 CFIR 签名边连接的函数声明簇。
 *
 * 簇保留所有语义参与者；诊断层另行选择唯一源码代表，不会从冲突图中删除
 * synthetic、继承或宏生成符号，因此代表诊断仍能携带完整的冲突成员信息。
 */
internal data class CfirFunctionConflictGroup(
    val signature: CfirFunctionConflictSignature,
    val members: Set<CfirBasedSymbol<*>>,
) {
    val presentation: String get() = signature.presentation
}

/**
 * 仓颉前端用来区分同名函数冲突簇的完整声明类别。
 *
 * 展示签名只包含函数名与参数类型；语义分组还必须保留 `static`、构造器和程序入口属性。
 * 未绑定符号的 static 属性未知，因此不会与任一已知 static 状态的簇合并。
 */
internal class CfirFunctionConflictSignature(
    val presentation: String,
    val callableId: CallableId,
    val dispatchReceiverType: ConeCangJieType?,
    val parameterTypes: List<ConeCangJieType>,
    val isStatic: Boolean,
    val isConstructor: Boolean,
    val isMainEntry: Boolean,
) {
    override fun equals(other: Any?): Boolean =
        other is CfirFunctionConflictSignature &&
                callableId == other.callableId &&
                dispatchReceiverType == other.dispatchReceiverType &&
                parameterTypes == other.parameterTypes &&
                isStatic == other.isStatic &&
                isConstructor == other.isConstructor &&
                isMainEntry == other.isMainEntry

    override fun hashCode(): Int {
        var result = callableId.hashCode()
        result = 31 * result + (dispatchReceiverType?.hashCode() ?: 0)
        result = 31 * result + parameterTypes.hashCode()
        result = 31 * result + isStatic.hashCode()
        result = 31 * result + isConstructor.hashCode()
        result = 31 * result + isMainEntry.hashCode()
        return result
    }
}

/** 一次诊断收集内跨文件共享的函数冲突簇去重状态。 */
internal class CfirFunctionConflictReportState {
    companion object {
        val KEY = DiagnosticCollectionStateKey(CfirFunctionConflictReportState::class.java)
    }

    private val reportedGroups = ConcurrentHashMap.newKeySet<IdentityBasedFunctionConflictGroup>()

    /** 仅允许同一组 CFIR symbol identity 的歧义簇报告一次。 */
    fun tryMarkReported(members: Set<CfirBasedSymbol<*>>): Boolean =
        reportedGroups.add(IdentityBasedFunctionConflictGroup(members))
}

/** 按 CFIR symbol 对象身份比较成员，避免声明模型的值相等语义改变诊断簇身份。 */
private class IdentityBasedFunctionConflictGroup(members: Set<CfirBasedSymbol<*>>) {
    private val members = Collections.newSetFromMap(IdentityHashMap<CfirBasedSymbol<*>, Boolean>()).apply {
        addAll(members)
    }

    override fun equals(other: Any?): Boolean =
        other is IdentityBasedFunctionConflictGroup &&
                members.size == other.members.size &&
                members.all { member -> other.members.any { it === member } }

    override fun hashCode(): Int = members.sumOf(System::identityHashCode)
}

/** 识别必须使用完整函数签名语义分组的 CFIR 符号类别。 */
internal fun isFunctionConflictParticipant(symbol: CfirBasedSymbol<*>): Boolean =
    symbol is CfirConstructorSymbol || symbol is CfirFunctionSymbol<*> || symbol is CfirEnumConstructorSymbol

/** 为函数冲突图计算与仓颉声明类别一致的签名。 */
internal fun functionConflictSignature(symbol: CfirBasedSymbol<*>): CfirFunctionConflictSignature? {
    if (!isFunctionConflictParticipant(symbol) || !symbol.isBound) return null
    val callable = symbol as? CfirCallableSymbol<*> ?: return null
    val presentation = CfirRedeclarationPresenter.represent(symbol) ?: return null
    val redeclarationTypes = callable.cfir.redeclarationTypesForConflict() ?: return null
    return CfirFunctionConflictSignature(
        presentation = presentation,
        callableId = callable.callableId,
        dispatchReceiverType = redeclarationTypes.dispatchReceiverType,
        parameterTypes = redeclarationTypes.parameterTypes,
        isStatic = callable.cfir.status.isStatic,
        isConstructor = symbol is CfirConstructorSymbol || symbol is CfirEnumConstructorSymbol,
        isMainEntry = symbol is CfirMainFunctionSymbol,
    )
}

/**
 * 从声明冲突图构建无向同签名函数簇。
 *
 * 冲突收集器的不同入口可能先后发现一条边的两个方向。这里统一按无向关系合并，
 * 以便文件、class-like 与 extend reporter 基于同一完整冲突簇做诊断代表选择。
 */
internal class CfirFunctionConflictGroups(
    declarationConflictingSymbols: Map<CfirBasedSymbol<*>, SmartSet<CfirBasedSymbol<*>>>,
) {
    private val groupsBySymbol = IdentityHashMap<CfirBasedSymbol<*>, CfirFunctionConflictGroup>()

    val groups: List<CfirFunctionConflictGroup>

    init {
        val adjacency = IdentityHashMap<CfirBasedSymbol<*>, MutableSet<CfirBasedSymbol<*>>>()
        val signatures = IdentityHashMap<CfirBasedSymbol<*>, CfirFunctionConflictSignature>()

        fun signatureOf(symbol: CfirBasedSymbol<*>): CfirFunctionConflictSignature? {
            if (!isFunctionConflictParticipant(symbol)) return null
            return signatures[symbol] ?: functionConflictSignature(symbol)
                ?.also { signatures[symbol] = it }
        }

        fun connect(first: CfirBasedSymbol<*>, second: CfirBasedSymbol<*>, signature: CfirFunctionConflictSignature) {
            adjacency.getOrPut(first, ::identitySet).add(second)
            adjacency.getOrPut(second, ::identitySet).add(first)
            signatures[first] = signature
            signatures[second] = signature
        }

        declarationConflictingSymbols.forEach { (declaration, conflictingSymbols) ->
            val signature = signatureOf(declaration) ?: return@forEach
            conflictingSymbols.forEach { conflictingSymbol ->
                if (signatureOf(conflictingSymbol) == signature) {
                    connect(declaration, conflictingSymbol, signature)
                }
            }
        }

        val visited = identitySet<CfirBasedSymbol<*>>()
        val builtGroups = mutableListOf<CfirFunctionConflictGroup>()
        adjacency.keys.forEach { start ->
            if (!visited.add(start)) return@forEach

            val members = identitySet<CfirBasedSymbol<*>>()
            val pending = ArrayDeque<CfirBasedSymbol<*>>()
            pending.addLast(start)
            while (pending.isNotEmpty()) {
                val current = pending.removeLast()
                if (!members.add(current)) continue
                adjacency[current].orEmpty().forEach { neighbor ->
                    if (visited.add(neighbor)) pending.addLast(neighbor)
                }
            }

            if (members.size < 2) return@forEach
            val signature = signatures.getValue(start)
            val group = CfirFunctionConflictGroup(signature, Collections.unmodifiableSet(members))
            builtGroups += group
            members.forEach { groupsBySymbol[it] = group }
        }
        groups = builtGroups
    }

    fun groupFor(symbol: CfirBasedSymbol<*>): CfirFunctionConflictGroup? = groupsBySymbol[symbol]

    private fun <T : Any> identitySet(): MutableSet<T> =
        Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
}

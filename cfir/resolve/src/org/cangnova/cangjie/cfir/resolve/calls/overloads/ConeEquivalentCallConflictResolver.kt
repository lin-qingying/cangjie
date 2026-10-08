package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/**
 * 过滤**等价调用**的冲突消解器。
 *
 * 对齐 Kotlin FIR `ConeEquivalentCallConflictResolver`：同一份声明可能经由多个符号提供器
 * 以不同身份进入候选集（多版本库、内建与 stdlib 重复、以及 Analysis API 的**文件副本**
 * 与上下文模块同时可见），它们在语义上是同一个声明，不应报「重载歧义」。
 * 缺了这一步，dangling 副本在 PREFER_SELF 下解析顶层声明会得到两个等价候选而判歧义。
 *
 * 判据（与 Kotlin 逐条对齐）：
 * 1. 只处理**非成员** callable（`containingClassLookupTag() == null`）；
 * 2. callableId 必须相同；
 * 3. **同一源码模块内的重声明永远不等价**——同模块重复声明该报重声明/冲突重载，
 *    不得在这里被吞掉；仅当 moduleData 不同（或 moduleData 声明「重声明可等价」，
 *    例如库源码模块）时才继续比较；
 * 4. 函数与属性不能互为等价；
 * 5. 实参到值参数的映射顺序必须一致；
 * 6. 签名按 alpha-equivalence 比较。
 *
 * **偏差一（签名比较手段）**：Kotlin 用 `FirStandardOverrideChecker.isOverriddenFunction/Property(..., ignoreVisibility = true)`
 * 双向判定签名；仓颉尚未抽出完整 override checker，改用 providers 层既有的
 * [overrideSignatureKey]——它同样只覆盖「名字 + 类型参数按位归一化 + 形参类型」
 * （不含返回类型，与 Kotlin 的判据一致），是仓颉侧「同一签名」的规范表示。
 *
 * **偏差二（无 session 状态）**：Kotlin 的解析器持有 session 供 override checker 构造类型
 * 上下文；仓颉的签名键比较不消费 session，故本消解器无状态，写成 object
 * （与仓颉侧其它无状态消解器一致）。行为规则不受影响。
 *
 * 另注：仓颉 extend 成员同样没有 class lookup tag，因此与 Kotlin 的顶层扩展函数一致地
 * 落入本规则；跨模块的 extend 同名同签名成员会被视为等价并保留先到者（与 Kotlin 对顶层
 * 扩展函数的处置一致），这是有意为之，不是漏判。
 */
object ConeEquivalentCallConflictResolver : ConeCallConflictResolver() {
    /**
     * 从候选集合中去掉等价重复项。
     *
     * 按「源码优先」重排后再去重，保证源码声明与二进制声明等价时保留源码声明（K1 兼容要求）。
     */
    override fun chooseMaximallySpecificCandidates(candidates: Set<Candidate>): Set<Candidate> =
        filterOutEquivalentCalls(candidates)

    /**
     * 依次处理候选；等价于已保留候选者直接丢弃。
     */
    private fun filterOutEquivalentCalls(candidates: Collection<Candidate>): Set<Candidate> {
        // 源码侧声明优先：等价的一对里先到者胜出，K1 兼容要求保源码。
        val fromSourceFirst = candidates.sortedBy {
            it.symbol.cfir.moduleData.session.kind != CfirSession.Kind.Source
        }

        val result = mutableSetOf<Candidate>()
        outerLoop@ for (myCandidate in fromSourceFirst) {
            val me = myCandidate.symbol.cfir
            if (me is CfirCallableDeclaration && me.containingClassLookupTag() == null) {
                val resultIterator = result.iterator()
                while (resultIterator.hasNext()) {
                    val otherCandidate = resultIterator.next()
                    val other = otherCandidate.symbol.cfir
                    if (other is CfirCallableDeclaration && other.containingClassLookupTag() == null) {
                        val callablesAreEquivalent = areEquivalentTopLevelCallables(me, other) {
                            myCandidate.mappedArgumentsOrderRepresentation.contentEquals(
                                otherCandidate.mappedArgumentsOrderRepresentation,
                            )
                        }
                        if (callablesAreEquivalent) {
                            // 仓颉没有 Kotlin 的 expect/actual 关系，等价时一律保留先到的候选。
                            continue@outerLoop
                        }
                    }
                }
            }
            result += myCandidate
        }
        return result
    }

    /**
     * 候选实参到值参数的映射顺序表示。
     *
     * 若候选是函数，则是一个数组：第 0 位是值参数个数，其后按调用实参顺序给出
     * 各实参对应的值参数下标；非函数候选为 `null`。未初始化映射时同样为 `null`。
     */
    private val Candidate.mappedArgumentsOrderRepresentation: IntArray?
        get() {
            val function = symbol.cfir as? CfirFunction ?: return null
            val parametersToIndices = function.valueParameters.mapIndexed { index, parameter ->
                parameter to index
            }.toMap()
            if (!argumentMappingInitialized) return null
            val mapping = argumentMapping
            val result = IntArray(mapping.size + 1) { function.valueParameters.size }
            for ((index, parameter) in mapping.values.withIndex()) {
                result[index + 1] = parametersToIndices[parameter]
                    ?: error("Unmapped argument in arguments mapping")
            }
            return result
        }

    /**
     * 两个非成员 callable 是否等价（即同一份声明经不同模块身份出现两次）。
     *
     * `argumentMappingIsEqual` 为实参映射顺序的一致性判定；为 `null` 表示不比较映射。
     */
    fun areEquivalentTopLevelCallables(
        first: CfirCallableDeclaration,
        second: CfirCallableDeclaration,
        argumentMappingIsEqual: (() -> Boolean)?,
    ): Boolean {
        if (first.symbol.callableId != second.symbol.callableId) return false

        // 同一源码模块内的重声明永远不等价：这里要保持「重定义 / 冲突重载」诊断可见。
        // 不能假定库声明的 moduleData 一定不同——回退依赖模块会让多份库在同一模块身份下出现，
        // 因此 moduleData 还提供 `areRedeclarationsEquivalent` 作为例外开关。
        if (first.moduleData == second.moduleData && !first.moduleData.areRedeclarationsEquivalent) return false

        if ((first is CfirProperty) != (second is CfirProperty)) {
            return false
        }
        if (argumentMappingIsEqual?.invoke() == false) {
            return false
        }

        val firstKey = first.signatureKeyOrNull() ?: return false
        return firstKey == second.signatureKeyOrNull()
    }

    /**
     * 取规范化签名键；未绑定或未解析的声明不参与等价判定。
     *
     * 未解析时 [overrideSignatureKey] 会退化成与类型无关的文本，可能把两份不同的声明
     * 判成等价；这里宁可保持歧义也不误合并。
     */
    private fun CfirCallableDeclaration.signatureKeyOrNull(): String? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        if (!callableSymbol.isBound) return null
        return callableSymbol.overrideSignatureKey()
    }
}

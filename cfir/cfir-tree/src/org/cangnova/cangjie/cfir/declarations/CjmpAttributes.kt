package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.hasCjmpDefaultImplementation
import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.patterns.simpleVariableBindingOrNull
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchFacts
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchPlan
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import java.util.Collections
import java.util.LinkedHashMap

private object CfirCjmpDeclarationMatchFactsKey : CfirDeclarationDataKey()

var CfirDeclaration.cjmpMatchFacts: CfirCjmpDeclarationMatchFacts?
    by CfirDeclarationDataRegistry.data(CfirCjmpDeclarationMatchFactsKey)

/** 阶段根独立发布的完整输入；访问器和 pattern binding 保留其真实符号，不分别回写子声明。 */
class CfirCjmpDeclarationMatchFacts(
    val rootSymbol: CfirBasedSymbol<*>,
    ownedSymbols: List<CfirBasedSymbol<*>>,
    plans: List<CjmpMatchPlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>>,
    val container: CfirCjmpContainerInput?,
    commonContainers: List<CfirCjmpContainerInput>,
) {
    val ownedSymbols: List<CfirBasedSymbol<*>> = immutableCjmpList(ownedSymbols)
    val plans: List<CjmpMatchPlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>> = CjmpMatchFacts.create(plans).plans
    val commonContainers: List<CfirCjmpContainerInput> = immutableCjmpList(commonContainers)

    init {
        require(ownedSymbols.any { it === rootSymbol }) { "A phase root must own its own symbol" }
        require(ownedSymbols.distinct().size == ownedSymbols.size) { "A declaration symbol may occur once in a phase root" }
        require(plans.all { plan -> ownedSymbols.any { it === plan.specific } }) {
            "Every declaration plan must belong to its publishing phase root"
        }
    }
}

/** 由当前根的匹配发布入口保存完整输入，不能将组级求值结果写入此属性。 */
fun CfirDeclaration.publishCjmpMatchFacts(
    facts: CfirCjmpDeclarationMatchFacts,
) {
    check(facts.rootSymbol === symbol) { "CJMP facts must be published by their phase root" }
    check(cjmpMatchFacts == null) { "CJMP facts have already been published for this declaration state" }
    cjmpMatchFacts = facts
}

private object CfirCjmpDeclarationMatchResultKey : CfirDeclarationDataKey()

/** specific 声明在 CJMP_MATCHING 阶段发布的有序对应关系及其类型参数映射。 */
var CfirDeclaration.cjmpMatchResult: CfirCjmpDeclarationMatchResult?
    by CfirDeclarationDataRegistry.data(CfirCjmpDeclarationMatchResultKey)

class CfirCjmpCounterpartMatch(
    val commonSymbol: CfirBasedSymbol<*>,
    typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
) {
    val typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter> = Collections.unmodifiableMap(
        LinkedHashMap(typeParameterMapping),
    )
}

class CfirCjmpDeclarationMatchResult(
    val specificSymbol: CfirBasedSymbol<*>,
    counterparts: List<CfirCjmpCounterpartMatch>,
) {
    val counterparts: List<CfirCjmpCounterpartMatch> = immutableCjmpList(counterparts)
    val commonSymbols: List<CfirBasedSymbol<*>> = immutableCjmpList(counterparts.map { it.commonSymbol })

    init {
        require(counterparts.map { it.commonSymbol }.distinct().size == counterparts.size) {
            "A declaration match result may contain each common counterpart once"
        }
    }
}

/** 配对阶段只发布该声明自身的最终结果；session 聚合仍负责产生稳定的组级裁决。 */
fun CfirDeclaration.publishCjmpMatchResult(result: CfirCjmpDeclarationMatchResult) {
    check(result.specificSymbol === symbol) { "CJMP result must be published by its specific declaration" }
    check(cjmpMatchResult == null) { "CJMP result has already been published for this declaration state" }
    cjmpMatchResult = result
}

private object FromCommonPartKey : CfirDeclarationDataKey()

private var CfirDeclaration.fromCommonPartAttribute: Boolean? by CfirDeclarationDataRegistry.data(FromCommonPartKey)

/**
 * 声明是否来自已序列化的 common part，与声明自身的 common 修饰角色相互独立。
 *
 * 官方 ASTWriter 为 common part 的全部声明写 FROM_COMMON_PART，包括普通 helper；
 * 该来源位不能赋予 COMMON 候选资格。源码尚未经过 common part 序列化时为 false。
 */
var CfirDeclaration.isFromCommonPart: Boolean
    get() = fromCommonPartAttribute == true
    set(value) {
        fromCommonPartAttribute = value
    }

/** 原始 COMMON_WITH_DEFAULT 语言事实独立于任何 specific session 的匹配结果。 */
fun CfirDeclaration.cjmpHasCommonDefault(): Boolean {
    val status = (this as? CfirMemberDeclaration)?.status ?: return false
    return hasCjmpDefaultImplementation(status.isSpecific) {
        // common CJO 保存的是生产端已计算的属性位；反序列化后的声明结构不能重新推导此位。
        if (isFromCommonPart) return@hasCjmpDefaultImplementation status.isCommonWithDefault
        if (status.isCommonWithDefault) return@hasCjmpDefaultImplementation true
        when (this) {
            is CfirErrorPrimaryConstructor -> false
            is CfirConstructor -> body != null && if (isPrimary) {
                origin != CfirDeclarationOrigin.ImplicitDefault &&
                    source?.kind != CjFakeSourceElementKind.ImplicitConstructor &&
                    moduleData.session.languageVersionSettings.supportsCjmpFeature(
                        CjmpLanguageFeature.PrimaryConstructorDefaultImplementation,
                    )
            } else true
            is CfirNamedFunction -> body != null
            is CfirFinalizer -> body != null
            is CfirPropertyAccessor -> body != null
            is CfirProperty -> getter != null || setter != null
            is CfirFieldVariable -> initializer != null
            is CfirPatternVariable -> pattern.simpleVariableBindingOrNull() != null && initializer != null
            is CfirTypeAlias -> false
            is CfirClassLikeDeclaration, is CfirExtend -> checkNotNull(cjmpSourceNominalHasDefault) {
                "Source nominal declaration is missing its parser default fact."
            }
            else -> false
        }
    }
}

/**
 * 原生 COMMON_WITH_DEFAULT 属性的统一查询，与 HasDefault 的 SPECIFIC 豁免保持分离。
 *
 * v1.1.3 ParseCJMPDecl.cpp 的 SetCJMPAttrs 先要求 COMMON，再计算 HasDefault。
 * 源码使用同一声明实现事实；common CJO 的该事实由 cjmpHasCommonDefault 原样读取
 * 序列化位，不能要求源码 raw status 已经拥有只在 CJO 写侧派生的存储位。
 */
fun CfirDeclaration.hasCjmpCommonDefaultAttribute(): Boolean =
    (this as? CfirMemberDeclaration)?.status?.isCommon == true && cjmpHasCommonDefault()

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirStructImpl
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpMemberSnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpStructureSnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping

/** CJMP 阶段只驱动逐声明事实发布；结果的生命周期由声明/session 失效管理，不再清空共享配对表。 */
internal class CfirCjmpMatchingProcessor(session: CfirSession, scopeSession: ScopeSession) : CfirTransformerBasedResolveProcessor(
    session = session, scopeSession = scopeSession, phase = CfirResolvePhase.CJMP_MATCHING,
) {
    override val transformer: CfirTransformer<Nothing?> = CfirCjmpMatcherTransformer(session, scopeSession)

    override fun processFile(file: CfirFile) {
        if (!session.isCjmpSpecificCompilationEnabled()) return
        super.processFile(file)
    }
}

/** alpha.66 在隐式类型阶段之后执行完整匹配；该处理器仅为该协议版本生效。 */
internal class CfirCjmpFinalMatchingProcessor(session: CfirSession, scopeSession: ScopeSession) : CfirTransformerBasedResolveProcessor(
    session = session, scopeSession = scopeSession, phase = CfirResolvePhase.CJMP_FINAL_MATCHING,
) {
    override val transformer: CfirTransformer<Nothing?> = CfirCjmpMatcherTransformer(
        session,
        scopeSession,
        CfirResolvePhase.CJMP_FINAL_MATCHING,
    )

    override fun processFile(file: CfirFile) {
        if (!session.requiresDeferredCjmpFinalMatching()) return
        super.processFile(file)
    }
}

/** 对位 Kotlin 的单目标发布入口；空候选同样发布，property/pattern 的子事实由当前根统一拥有。 */
open class CfirCjmpMatcherTransformer(
    override val session: CfirSession,
    private val scopeSession: ScopeSession,
    private val matchingPhase: CfirResolvePhase = CfirResolvePhase.CJMP_MATCHING,
) : CfirAbstractTreeTransformer<Nothing?>(matchingPhase) {
    override fun transformFile(file: CfirFile, data: Nothing?): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            file.transformDeclarations(this, data)
            file
        }
    }

    fun transformMemberDeclaration(
        memberDeclaration: CfirMemberDeclaration,
        preparedFacts: CfirCjmpDeclarationMatchFacts? = null,
        preparedFamilySnapshot: CfirCjmpFamilySnapshot? = null,
        preparedStructureSnapshot: CfirCjmpStructureSnapshot? = null,
    ) {
        if (!session.isCjmpSpecificCompilationEnabled()) return
        if (matchingPhase == CfirResolvePhase.CJMP_MATCHING && session.requiresDeferredCjmpFinalMatching()) {
            if (memberDeclaration is CfirClassLikeDeclaration || memberDeclaration is CfirExtend) {
                val structure = preparedStructureSnapshot ?: session.cjmpFamilyAggregate.structureFor(memberDeclaration)
                memberDeclaration.addSpecificDefaultConstructorAfterMerge(structure)
            }
            return
        }
        val existingFacts = memberDeclaration.cjmpMatchFacts
        val specificRoot = memberDeclaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) &&
                CfirCjmpMatchRunner.canHaveCommonCounterpart(memberDeclaration)
        if (existingFacts != null && (!specificRoot || memberDeclaration.cjmpMatchResult != null)) return
        val facts = preparedFacts ?: CfirCjmpMatchRunner.prepareDeclaration(memberDeclaration, session)
        check(facts.rootSymbol === memberDeclaration.symbol) { "Prepared CJMP facts must belong to this phase root" }
        val family = if (specificRoot) {
            preparedFamilySnapshot ?: session.cjmpFamilyAggregate.matchingGroupFor(memberDeclaration, facts)
        } else {
            null
        }
        if (family != null) memberDeclaration.addSpecificDefaultConstructorAfterMerge(family)
        val results = if (family == null) emptyList() else facts.ownedSymbols.map { ownedSymbol ->
            val ownedDeclaration = ownedSymbol.cfir as? CfirDeclaration
                ?: error("Every declaration-owned CJMP symbol must reference a declaration")
            ownedDeclaration to family.declarationMatchResultFor(ownedDeclaration)
        }
        check(results.all { (declaration, _) -> declaration.cjmpMatchResult == null }) {
            "CJMP declaration results must be published as one phase transaction"
        }
        if (existingFacts == null) memberDeclaration.publishCjmpMatchFacts(facts)
        results.forEach { (declaration, result) -> declaration.publishCjmpMatchResult(result) }
    }

    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E = element

    /** 转换 class 声明前执行配对。 */
    override fun transformClass(klass: CfirClass, data: Nothing?): CfirClass {
        transformMemberDeclaration(klass)
        klass.transformDeclarations(this, data)
        return klass
    }

    /** 转换 struct 声明前执行配对。 */
    override fun transformStruct(struct: CfirStruct, data: Nothing?): CfirStruct {
        transformMemberDeclaration(struct)
        struct.transformDeclarations(this, data)
        return struct
    }

    /** 转换 interface 声明前执行配对。 */
    override fun transformInterface(`interface`: CfirInterface, data: Nothing?): CfirInterface {
        transformMemberDeclaration(`interface`)
        `interface`.transformDeclarations(this, data)
        return `interface`
    }

    /** 转换 enum 声明前执行配对。 */
    override fun transformEnum(enum: CfirEnum, data: Nothing?): CfirEnum {
        transformMemberDeclaration(enum)
        enum.transformDeclarations(this, data)
        return enum
    }

    /** 转换 extend 前执行配对（官方 `MergeCJMPExtensions`：扩展类型 + 接口集为键）。 */
    override fun transformExtend(
        extend: org.cangnova.cangjie.cfir.declarations.CfirExtend,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirExtend {
        transformMemberDeclaration(extend)
        extend.transformDeclarations(this, data)
        return extend
    }

    /** 转换命名函数前执行配对。 */
    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: Nothing?) = run {
        transformMemberDeclaration(namedFunction)
        namedFunction
    }

    /** 转换构造器前执行配对（secondary init；static init 由 parse 规则单列，匹配器内部跳过）。 */
    override fun transformConstructor(constructor: CfirConstructor, data: Nothing?) = run {
        transformMemberDeclaration(constructor)
        constructor
    }

    /** enum 构造器在自己的声明级入口按官方 CJMP enum-constructor 规则配对。 */
    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: Nothing?): CfirEnumConstructor {
        transformMemberDeclaration(enumConstructor)
        return enumConstructor
    }

    /** 转换属性前执行配对。 */
    override fun transformProperty(property: CfirProperty, data: Nothing?): CfirProperty {
        transformMemberDeclaration(property)
        return property
    }

    /** 转换变量前执行配对（官方 `MatchCJMPVar`；实例成员使用已配对父容器的泛型映射）。 */
    override fun transformFieldVariable(
        fieldVariable: org.cangnova.cangjie.cfir.declarations.CfirFieldVariable,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirFieldVariable {
        transformMemberDeclaration(fieldVariable)
        return fieldVariable
    }

    /** 转换顶层模式变量前执行配对（`let v: T = e` 以模式变量承载；官方 `MatchCJMPVar` / 元组模式逐变量配对）。 */
    override fun transformPatternVariable(
        patternVariable: org.cangnova.cangjie.cfir.declarations.CfirPatternVariable,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirPatternVariable {
        transformMemberDeclaration(patternVariable)
        return patternVariable
    }

}

/** 官方在 common 成员并入后才为仍无实例构造器的 specific class/struct 补默认构造器。 */
private fun CfirMemberDeclaration.addSpecificDefaultConstructorAfterMerge(family: CfirCjmpMemberSnapshot) {
    val owner = this as? CfirClassLikeDeclaration ?: return
    if (owner !is CfirClass && owner !is CfirStruct) return
    if (!owner.status.isSpecific || !owner.canGenerateDefaultInstanceConstructor()) return

    val mergedMembers = family.mergedDeclarationsFor(owner)
    if (mergedMembers.any { it is CfirConstructor && !it.status.isStatic }) return

    val mutableDeclarations = when (owner) {
        is CfirClass -> (owner as? CfirClassImpl)?.declarations
        is CfirStruct -> (owner as? CfirStructImpl)?.declarations
        else -> null
    } ?: return
    if (mutableDeclarations.any { it is CfirConstructor && it.origin == CfirDeclarationOrigin.ImplicitDefault }) return

    val constructor = owner.createDefaultInstanceConstructor()
    constructor.completeConstructorReturnType(owner.constructorOwnerType())
    // 此构造器借用的 constructed-class 参数已在 owner TYPES 完成；返回引用也必须先实际完成。
    constructor.replaceResolvePhase(CfirResolvePhase.TYPES)
    mutableDeclarations += constructor
}

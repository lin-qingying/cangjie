package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.session.CfirCjmpMemberSnapshot
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import java.util.Collections

/**
 * 官方 DeclarationPreCheck 的声明输入 owner。
 *
 * 声明检查读取未应用 doNotExport 的原始/合并声明，按成员自身位过滤函数名字组。
 * 调用实际化及泛型实例化拥有各自候选表，不能消费这里的过滤结果。
 */
interface CfirDeclarationPreCheckCandidateProvider : CfirSessionComponent {
    /** 请求明确的输入阶段；FINAL 不能退回 STRUCTURE，局部声明使用独立词法作用域。 */
    fun inputFor(
        owner: CfirDeclaration,
        scopeSession: ScopeSession,
        view: CfirDeclarationPreCheckView,
    ): CfirDeclarationPreCheckInput
}

/** 结构合并、最终匹配与普通声明输入的缓存域彼此独立。 */
enum class CfirDeclarationPreCheckView { RAW, CJMP_STRUCTURE, CJMP_FINAL }

/** 名字组的作用域身份；同包文件共享身份，名义类型/extend 使用真实逻辑声明 symbol。 */
sealed interface CfirDeclarationPreCheckScopeIdentity {
    data class Package(val session: CfirSession, val packageFqName: FqName) : CfirDeclarationPreCheckScopeIdentity
    data class Member(val owner: CfirBasedSymbol<*>) : CfirDeclarationPreCheckScopeIdentity
}

/** 对位官方 Names：规范化名字与作用域身份分别保存，不能拼接成展示文本。 */
data class CfirDeclarationPreCheckNames(
    val name: Name,
    val scope: CfirDeclarationPreCheckScopeIdentity,
)

/** 官方 FUNC_DECL 在 CFIR 中的真实类别；访问器与 lambda 不进入函数组。 */
enum class CfirDeclarationPreCheckFunctionKind { FUNCTION, CONSTRUCTOR, ENUM_CONSTRUCTOR, MAIN, MACRO, FINALIZER }

/** 来源文件身份用于顶层 private 豁免；CJO 文件身份保留声明模块，不猜测 use-site 文件。 */
sealed interface CfirDeclarationPreCheckSourceFileIdentity {
    data class Source(val file: CfirFile) : CfirDeclarationPreCheckSourceFileIdentity
    data class Binary(val moduleData: CfirModuleData, val path: String) : CfirDeclarationPreCheckSourceFileIdentity
}

/** 候选的自身语言属性；物理 owner 的角色和 FROM_COMMON_PART 都不能代替 COMMON。 */
data class CfirDeclarationPreCheckAttributes(
    val isCommon: Boolean,
    val isSpecific: Boolean,
    val isFromCommonPart: Boolean,
    val isStatic: Boolean,
    val isForeign: Boolean,
    val isC: Boolean,
)

/** 原声明、布局投影与诊断来源的不可变关联；不复制 CFIR 树。 */
data class CfirDeclarationPreCheckCandidate(
    val declaration: CfirDeclaration,
    val names: CfirDeclarationPreCheckNames?,
    val functionKind: CfirDeclarationPreCheckFunctionKind?,
    val attributes: CfirDeclarationPreCheckAttributes,
    val physicalOwner: CfirDeclaration?,
    val logicalOwner: CfirDeclaration?,
    val ownerTypeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    val sourceFileIdentity: CfirDeclarationPreCheckSourceFileIdentity?,
    val origin: CfirDeclarationOrigin,
    val declarationOrder: Int,
    val sourceStartOffset: Int?,
    val sourceEndOffset: Int?,
)

/** 各专用函数检查共用同一过滤后组，同时保留未过滤输入用于核验。 */
class CfirDeclarationPreCheckFunctionGroup(
    val names: CfirDeclarationPreCheckNames,
    rawCandidates: List<CfirDeclarationPreCheckCandidate>,
    candidates: List<CfirDeclarationPreCheckCandidate>,
) {
    val rawCandidates: List<CfirDeclarationPreCheckCandidate> = frozenPreCheckList(rawCandidates)
    val candidates: List<CfirDeclarationPreCheckCandidate> = frozenPreCheckList(candidates)
}

/** 单次作用域输入；general 重定义消费 rawCandidates，不套函数整组 COMMON 删除。 */
class CfirDeclarationPreCheckInput(
    val scope: CfirDeclarationPreCheckScopeIdentity,
    val view: CfirDeclarationPreCheckView,
    val snapshot: CfirCjmpMemberSnapshot?,
    rawCandidates: List<CfirDeclarationPreCheckCandidate>,
    functionGroups: List<CfirDeclarationPreCheckFunctionGroup>,
) {
    val rawCandidates: List<CfirDeclarationPreCheckCandidate> = frozenPreCheckList(rawCandidates)
    val functionGroups: List<CfirDeclarationPreCheckFunctionGroup> = frozenPreCheckList(functionGroups)
    val rawDeclarations: List<CfirDeclaration> = frozenPreCheckList(rawCandidates.map { it.declaration })
    val isCjmpActualized: Boolean get() = snapshot != null

    private val candidatesBySymbol = rawCandidates.associateBy { it.declaration.symbol }
    private val functionGroupsBySymbol = functionGroups.flatMap { group ->
        group.rawCandidates.map { it.declaration.symbol to group }
    }.toMap()

    val filteredCommonFunctions: Set<CfirBasedSymbol<*>> = Collections.unmodifiableSet(
        functionGroups.flatMap { group ->
            val retained = group.candidates.mapTo(hashSetOf()) { it.declaration.symbol }
            group.rawCandidates.filterNot { it.declaration.symbol in retained }.map { it.declaration.symbol }
        }.toSet(),
    )

    fun candidateFor(declaration: CfirDeclaration): CfirDeclarationPreCheckCandidate =
        candidatesBySymbol.getValue(declaration.symbol)

    fun functionGroupFor(declaration: CfirDeclaration): CfirDeclarationPreCheckFunctionGroup? =
        functionGroupsBySymbol[declaration.symbol]
}

val CfirSession.declarationPreCheckCandidateProvider: CfirDeclarationPreCheckCandidateProvider
    by CfirSession.sessionComponentAccessor()

private fun <T> frozenPreCheckList(values: List<T>): List<T> = Collections.unmodifiableList(ArrayList(values))

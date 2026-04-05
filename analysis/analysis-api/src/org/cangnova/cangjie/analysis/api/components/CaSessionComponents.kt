package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaSubstitutedSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.analysis.api.types.pointers.CaTypePointer
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression

// ===== 解析与符号 =====

/**
 * 引用与调用解析协议。
 *
 * Analysis API 对外暴露的解析入口统一收敛在这里。
 * 上层只观察“引用 -> 符号”和“调用点 -> 调用结果”的稳定语义，
 * 不直接依赖底层 CFIR 的候选对象或解析树实现。
 */
interface CaResolver : CaLifetimeOwner {
    /**
     * 解析引用表达式对应的目标符号集合。
     */
    fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol>

    /**
     * 当引用唯一解析到单个符号时直接返回该符号。
     */
    fun CjReferenceExpression.resolveToSymbol(): CaSymbol? = resolveToSymbols().singleOrNull()

    /**
     * 查询元素对应的调用语义快照。
     */
    fun CjElement.resolveToCall(): CaCallInfo?
}

/**
 * 符号关系协议。
 *
 * 该协议承载稳定的“语义同一性”判定，
 * 供作用域去重、补全、引用规划和会话级缓存复用。
 */
interface CaSymbolRelationProvider : CaLifetimeOwner {
    /**
     * 判断两个公开符号在当前 session 里是否指向同一语义实体。
     */
    fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean
}

/**
 * 符号查询协议。
 *
 * 当前稳定支持文件、包、class-like 与顶层 callable 的公开查询入口。
 */
interface CaSymbolProvider : CaLifetimeOwner {
    fun CjFile.fileSymbol(): CaFileSymbol

    fun getPackageSymbol(fqName: FqName): CaPackageSymbol?

    fun getClassLikeSymbol(classId: ClassId): CaClassLikeSymbol?

    /**
     * 查询指定包内、指定短名下的全部顶层 class-like 符号。
     */
    fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name): List<CaClassLikeSymbol>

    /**
     * 查询指定包内、指定短名下的全部顶层 callable 符号。
     */
    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CaCallableSymbol>
}

/**
 * 符号附加信息协议。
 */
interface CaSymbolInformationProvider : CaLifetimeOwner {
    fun CaSymbol.createPointer(): CaSymbolPointer<CaSymbol>
}

/**
 * 声明注解查询协议。
 *
 * 注解属于声明级公开语义的一部分，不应该停留在 PSI 拼装层。
 */
interface CaAnnotationProvider : CaLifetimeOwner {
    /**
     * 查询声明符号上直接声明的注解。
     */
    val CaDeclarationSymbol.annotations: List<CaAnnotation>
}

/**
 * 可调用签名查询协议。
 *
 * 签名是 IDE、LSP、渲染器和工具层共享的结构化视图，
 * 不应要求调用方重复拼接参数文本。
 */
interface CaSignatureProvider : CaLifetimeOwner {
    /**
     * 从源码声明提取结构化签名。
     */
    val CjCallableDeclaration.signature: CaSignature

    /**
     * 从公开 callable 符号提取结构化签名。
     *
     * 当当前符号没有稳定签名来源时返回 `null`。
     */
    val CaCallableSymbol.signature: CaSignature?
}

// ===== 类型 =====

/**
 * 类型查询协议。
 *
 * 该层暴露符号视角下的类型信息，例如函数返回类型、
 * class-like 默认类型和直接超类型。
 */
interface CaTypeProvider : CaLifetimeOwner {
    val CaCallableSymbol.returnType: CaType?

    val CaClassLikeSymbol.defaultType: CaType

    /**
     * 查询 class-like 声明的直接超类型。
     */
    val CaClassLikeSymbol.superTypes: List<CaType>
}

/**
 * 类型附加信息协议。
 */
interface CaTypeInformationProvider : CaLifetimeOwner {
    fun CaType.createPointer(): CaTypePointer<CaType>

    val CaType.isErrorType: Boolean

    /**
     * 查询当前类型是否能稳定映射到公开 class-like 符号。
     */
    val CaType.classLikeSymbol: CaClassLikeSymbol?
}

/**
 * 类型关系判定协议。
 *
 * Analysis API 既暴露 `CaType`，也必须暴露稳定的关系判断入口，
 * 让 IDE、LSP、重构和渲染层在同一套公开语义模型上工作。
 */
interface CaTypeRelationChecker : CaLifetimeOwner {
    /**
     * 判断当前类型是否是 [superType] 的子类型。
     */
    fun CaType.isSubTypeOf(superType: CaType): Boolean

    /**
     * 判断两个类型在当前 use-site session 下是否语义相等。
     */
    fun CaType.semanticallyEquals(other: CaType): Boolean
}

/**
 * 类型构造协议。
 *
 * `analysis-api` 不能只暴露“读取已有类型”的能力，
 * 还必须提供稳定的公开构造入口，供 IDE、LSP、重构与测试框架共用。
 */
interface CaTypeCreator : CaLifetimeOwner {
    /**
     * 基于可见 class-like 声明 ID 构造具名类型。
     */
    fun buildClassLikeType(
        classId: ClassId,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType

    /**
     * 基于公开 class-like 符号构造具名类型。
     */
    fun buildClassLikeType(
        symbol: CaClassLikeSymbol,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType

    /**
     * 构造函数类型 `(P1, P2, ...) -> R`。
     */
    fun buildFunctionType(
        parameterTypes: List<CaType>,
        returnType: CaType,
        isCFunction: Boolean = false,
        isClosureType: Boolean = false,
        hasVariableLengthArgument: Boolean = false,
    ): CaFunctionType

    /**
     * 构造元组类型 `(T1, T2, ...)`。
     */
    fun buildTupleType(
        elementTypes: List<CaType>,
    ): CaTupleType

    /**
     * 构造交叉类型 `A & B & ...`。
     */
    fun buildIntersectionType(
        conjuncts: List<CaType>,
    ): CaIntersectionType

    /**
     * 构造联合类型 `A | B | ...`。
     */
    fun buildUnionType(
        alternatives: Collection<CaType>,
    ): CaUnionType
}

/**
 * 类型替换器构建协议。
 *
 * Analysis API 需要显式暴露“按类型参数实例化语义类型”的能力，
 * 供渲染、补全、文档与调用结果展示复用。
 */
interface CaSubstitutorProvider : CaLifetimeOwner {
    /**
     * 基于显式类型参数替换表创建语义替换器。
     */
    fun createTypeSubstitutor(substitutions: Map<Name, CaType>): CaTypeSubstitutor

    /**
     * 基于签名中的类型参数顺序和给定实参列表创建实例化替换器。
     */
    fun CaSignature.createSubstitutor(typeArguments: List<CaType>): CaTypeSubstitutor
}

/**
 * 签名替换协议。
 */
interface CaSignatureSubstitutor : CaLifetimeOwner {
    /**
     * 对签名应用语义类型替换。
     */
    fun CaSignature.substitute(substitutor: CaTypeSubstitutor): CaSubstitutedSignature
}

// ===== 表达式 =====

/**
 * 表达式与声明类型查询协议。
 */
interface CaExpressionTypeProvider : CaLifetimeOwner {
    val CjExpression.expressionType: CaType?

    val CjCallableDeclaration.returnType: CaType?
}

/**
 * 表达式结构信息查询协议。
 *
 * 该层暴露表达式在公开语义中的稳定结构，
 * 不要求调用方自己判断 PSI 节点细节。
 */
interface CaExpressionInformationProvider : CaLifetimeOwner {
    /**
     * 判断表达式当前是否以语句形态参与控制流。
     */
    val CjExpression.isStatementLike: Boolean

    /**
     * 判断表达式是否能被视为编译期常量。
     */
    val CjExpression.isCompileTimeConstant: Boolean
}

/**
 * 编译期值求值协议。
 */
interface CaEvaluator : CaLifetimeOwner {
    fun CjExpression.evaluate(): CaCompileTimeValue?
}

/**
 * 数据流快照查询协议。
 */
interface CaDataFlowProvider : CaLifetimeOwner {
    /**
     * 读取表达式在当前 use-site session 下的数据流快照。
     */
    fun CjExpression.getDataFlowInfo(): CaDataFlowInfo
}

// ===== 诊断与作用域 =====

interface CaDiagnosticProvider : CaLifetimeOwner {
    fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>

    fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>
}

/**
 * 作用域查询协议。
 *
 * 当前稳定支持文件、包、声明成员、可见成员和类型作用域。
 */
interface CaScopeProvider : CaLifetimeOwner {
    fun CjFile.getFileScope(): CaScope

    fun getPackageScope(packageFqName: FqName): CaScope?

    val CaPackageSymbol.packageScope: CaScope

    val CaClassLikeSymbol.declaredMemberScope: CaScope

    val CaClassLikeSymbol.memberScope: CaScope

    val CaType.scope: CaScope?
}

interface CaAnalysisScopeProvider : CaLifetimeOwner {
    fun CaModule.analysisScope(): GlobalSearchScope
}

/**
 * 默认导入查询协议。
 *
 * 默认导入属于 use-site session 的解析环境，因此由 session 统一提供。
 */
interface CaDefaultImportProvider : CaLifetimeOwner {
    val defaultImports: CaDefaultImports
}

// ===== IDE / 工具支持 =====

/**
 * 补全候选过滤协议。
 *
 * 它只提供平台无关的语义判定，不直接构造 IDE 专属的 lookup 元素。
 */
interface CaCompletionCandidateChecker : CaLifetimeOwner {
    /**
     * 判断某个符号在指定位置是否可直接作为补全候选暴露，
     * 以及是否需要补一条 import。
     */
    fun CaSymbol.checkCompletionCandidate(position: CjElement): CaCompletionCandidateDecision
}

/**
 * 公开可见性判定协议。
 *
 * 这里的“可见”指在当前 use-site session 及其模块闭包下，
 * 符号是否仍能被稳定恢复并参与公开语义查询。
 */
interface CaVisibilityChecker : CaLifetimeOwner {
    /**
     * 判断当前公开符号在当前 use-site session 中是否可见。
     */
    fun CaSymbol.isVisible(): Boolean
}

/**
 * 引用缩短规划协议。
 *
 * 它只生成“哪些引用可以被缩短、是否需要补导入”的稳定计划，
 * 不直接修改 PSI。
 */
interface CaReferenceShortener : CaLifetimeOwner {
    /**
     * 为单个文件构建引用缩短计划。
     */
    fun CjFile.collectReferenceShorteningPlan(): CaReferenceShorteningPlan
}

/**
 * 导入优化规划协议。
 *
 * 它把当前文件的导入状态整理为 retained、duplicate、unused、missing 四类稳定集合，
 * 供 IDE、LSP 和批处理工具消费。
 */
interface CaImportOptimizer : CaLifetimeOwner {
    /**
     * 为单个文件构建导入优化计划。
     */
    fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan
}

/**
 * 符号与类型渲染协议。
 */
interface CaRenderer : CaLifetimeOwner {
    fun CaSymbol.render(): String

    fun CaType.render(): String
}

// ===== 源码与互操作 =====

interface CaOriginalPsiProvider : CaLifetimeOwner {
    /**
     * 获取当前公开符号在源码工程中的原始 PSI。
     */
    fun CaSymbol.getOriginalPsi(): PsiElement?
}

interface CaSourceProvider : CaLifetimeOwner {
    fun CaSymbol.getContainingFile(): CjFile?
}

/**
 * 互操作语义查询协议。
 *
 * 该协议统一暴露源码中的 `foreign` 边界、FFI 注解、外部名与调用约定。
 */
interface CaCInteropComponent : CaLifetimeOwner {
    /**
     * 查询某个源码元素所属声明边界的互操作语义。
     */
    fun CjElement.getInteropInfo(): CaInteropInfo?

    /**
     * 查询公开符号的互操作语义。
     */
    fun CaSymbol.getInteropInfo(): CaInteropInfo?
}

interface CaDocProvider : CaLifetimeOwner {
    fun CaSymbol.documentation(): String?
}

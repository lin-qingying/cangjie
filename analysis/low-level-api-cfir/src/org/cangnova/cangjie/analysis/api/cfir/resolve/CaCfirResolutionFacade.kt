package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * CFIR low-level 解析 facade。
 *
 * Analysis API 通过它访问底层 CFIR session、文件结构、语义快照与诊断结果，
 * 避免把 session 构建、Raw CFIR 构建和 resolve 细节泄漏到上层组件。
 */
enum class CaCfirCallKind {
    FUNCTION,
}

enum class CaCfirCallOrigin {
    REGULAR,
    OPERATOR,
    CONSTRUCTOR_DELEGATION_THIS,
    CONSTRUCTOR_DELEGATION_SUPER,

}

/**
 * low-level 调用候选适用性。
 *
 * 该枚举稳定镜像底层 resolver 的候选适用性，
 * 但隔离了具体的 `CandidateApplicability` 实现类型。
 */
enum class CaCfirCallApplicability {
    HIDDEN,
    INAPPLICABLE_WRONG_RECEIVER,
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    INAPPLICABLE,
    VISIBILITY_ERROR,
    UNSAFE_CALL,
    UNSTABLE_SMARTCAST,
    CONVENTION_ERROR,
    RESOLVED_LOW_PRIORITY,
    RESOLVED_NEED_PRESERVE_COMPATIBILITY,
    RESOLVED_WITH_ERROR,
    RESOLVED,
}

/**
 * low-level 单个源码实参与形参映射快照。
 */
data class CaCfirCallArgumentMappingSnapshot(
    val argumentIndex: Int,
    val parameterName: Name?,
    val parameterType: ConeCangJieType?,
)

/**
 * low-level CFIR 调用快照。
 *
 * 这里只保留 Analysis API 当前公开调用协议需要的稳定语义，
 * 不把 CFIR 的具体调用树或 candidate 对象直接暴露给上层。
 */
data class CaCfirCallSnapshot(
    val kind: CaCfirCallKind,
    val origin: CaCfirCallOrigin,
    val applicability: CaCfirCallApplicability,
    val isImplicitInvoke: Boolean,
    val calleeName: Name?,
    val target: CfirCallableSymbol<*>?,
    val explicitReceiverType: ConeCangJieType?,
    val dispatchReceiverType: ConeCangJieType?,
    val extensionReceiverType: ConeCangJieType?,
    val contextArgumentTypes: List<ConeCangJieType?>,
    val argumentTypes: List<ConeCangJieType?>,
    val typeArguments: List<ConeCangJieType?>,
    val argumentMapping: List<CaCfirCallArgumentMappingSnapshot>,
)

/**
 * low-level CFIR 调用结果快照。
 *
 * 公开 Analysis API 的 `CaCallInfo` 将基于它完成映射。
 */
data class CaCfirCallInfoSnapshot(
    val successfulCall: CaCfirCallSnapshot?,
    val calls: List<CaCfirCallSnapshot>,
)

/**
 * low-level 顶层公开符号查询结果。
 *
 * 同一包和短名下的 class-like 与 callable 应共享同一份查询快照，
 * 防止上层重复查询后再次决定顺序或去重规则。
 */
data class CaCfirTopLevelSymbolQueryResult(
    val classLikeSymbols: List<CfirClassLikeSymbol<*>>,
    val callableSymbols: List<CfirCallableSymbol<*>>,
)

interface CaCfirResolutionFacade {
    val useSiteModule: CaModule

    val useSiteFirSession: CfirSession

    /**
     * 当前 use-site 快照中 low-level 可见的模块闭包。
     *
     * 后续 scope、cache 与 invalidation 等设施都应围绕这组模块工作，
     * 而不是再次遍历 Analysis API 的模块依赖图。
     */
    val allModules: Set<CaModule>

    /**
     * 当前 use-site 模块已经构建完成的 CFIR 文件。
     */
    val cfirFiles: List<CfirFile>

    /**
     * 按 PSI 文件定位对应的 CFIR 文件。
     */
    fun getCfirFile(file: CjFile): CfirFile?

    /**
     * 按 PSI 文件定位对应的 low-level 文件符号。
     *
     * 文件符号同样通过 low-level 统一提供，避免上层维护额外特例。
     */
    fun getFileSymbol(file: CjFile): CfirFileSymbol?

    /**
     * 查询文件在当前 use-site 上下文中的 low-level 作用域快照。
     */
    fun getFileScope(file: CjFile): CaCfirScopeSnapshot

    /**
     * 查询包级 low-level 作用域快照。
     */
    fun getPackageScope(packageFqName: FqName): CaCfirScopeSnapshot?

    /**
     * 查询 class-like 自身的 declared-member 作用域快照。
     */
    fun getDeclaredMemberScope(classId: ClassId): CaCfirScopeSnapshot?

    /**
     * 查询 class-like 在当前 use-site 上下文中的可见成员作用域快照。
     */
    fun getMemberScope(classId: ClassId): CaCfirScopeSnapshot?

    /**
     * 查询类型在当前 use-site 上下文中的 type scope 快照。
     *
     * `type scope` 必须由 low-level 层统一提供，
     * 上层不能再自行从 `ClassId` 回退推导成员作用域。
     */
    fun getTypeScope(type: ConeCangJieType): CaCfirScopeSnapshot?

    /**
     * 按当前 use-site 模块闭包判断包是否可见。
     */
    fun hasPackage(packageFqName: FqName): Boolean

    /**
     * 按当前 use-site 模块闭包查询单个 class-like 符号。
     */
    fun getClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>?

    /**
     * 查询指定包、指定短名下的全部顶层 class-like 符号。
     */
    fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name): List<CfirClassLikeSymbol<*>>

    /**
     * 查询指定包、指定短名下的全部顶层 callable 符号。
     */
    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>>

    /**
     * 统一查询同一包和短名下的全部顶层公开符号。
     *
     * `analysis-api-cfir` 的顶层符号恢复应优先消费这一路径，
     * 而不是分别发起 class-like 与 callable 查询。
     */
    fun getTopLevelSymbols(packageFqName: FqName, name: Name): CaCfirTopLevelSymbolQueryResult

    /**
     * 在当前 use-site 模块闭包里回查底层符号对应的源码 PSI。
     */
    fun findSourcePsi(symbol: CfirSymbol<*>): PsiElement?

    fun getDeclarationSymbols(psi: PsiElement): List<CfirSymbol<*>>

    /**
     * 在当前 use-site 模块闭包里查询底层符号所属的源码文件。
     */
    fun getContainingFile(symbol: CfirSymbol<*>): CjFile?

    /**
     * 查询表达式的解析后类型。
     */
    fun getExpressionType(expression: CjExpression): ConeCangJieType?

    /**
     * 查询可调用声明的返回类型。
     */
    fun getDeclarationReturnType(declaration: CjCallableDeclaration): ConeCangJieType?

    /**
     * 查询值参数声明的语义类型。
     *
     * 公开签名模型不能退化成只暴露 `typeText` 的文本视图，
     * 因此 low-level 需要为参数类型提供稳定快照入口。
     */
    fun getValueParameterType(parameter: CjParameter): ConeCangJieType?

    /**
     * 查询类声明的默认类型。
     */
    fun getClassDefaultType(declaration: CjClassLikeDeclaration): ConeCangJieType?

    /**
     * 查询 callable 符号的返回类型。
     *
     * 由 low-level 层统一封装底层返回类型读取规则，
     * 上层 Analysis API 不直接读取 `resolvedReturnTypeRef`。
     */
    fun getCallableReturnType(symbol: CfirCallableSymbol<*>): ConeCangJieType?

    /**
     * 查询 class-like 符号的默认类型。
     *
     * `default type` 的封装也统一由 low-level 承担，
     * 上层只消费映射后的 `CaType`。
     */
    fun getClassLikeDefaultType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType?

    /**
     * 从 low-level 类型推导其对应的 class-like 符号。
     *
     * 该入口用于公开类型侧的 `classLikeSymbol` 恢复；
     * 无法稳定映射时返回 `null`。
     */
    fun getTypeClassLikeSymbol(type: ConeCangJieType): CfirClassLikeSymbol<*>?

    /**
     * 查询 class-like 符号的直接超类型集合。
     *
     * 公开 API 的 `CaClassLikeSymbol.superTypes` 由这里提供。
     */
    fun getClassLikeSuperTypes(symbol: CfirClassLikeSymbol<*>): List<ConeCangJieType>

    /**
     * 在 low-level 类型系统上下文中判断子类型关系。
     */
    fun isSubTypeOf(
        subType: ConeCangJieType,
        superType: ConeCangJieType,
    ): Boolean

    /**
     * 在 low-level 类型系统上下文中判断两个类型是否语义相等。
     */
    fun areTypesEqual(
        left: ConeCangJieType,
        right: ConeCangJieType,
    ): Boolean

    /**
     * 解析引用表达式对应的底层符号集合。
     */
    fun resolveReference(reference: CjReferenceExpression): Collection<CfirSymbol<*>>

    /**
     * 查询元素对应的调用快照。
     */
    fun getCallInfo(element: PsiElement): CaCfirCallInfoSnapshot?

    fun getDiagnostics(element: PsiElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic>

    fun collectDiagnosticsForFile(file: CjFile, filter: DiagnosticCheckerFilter): Collection<CjPsiDiagnostic>
}

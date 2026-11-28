package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

/** 原生 Decl 的 linkage；与语言 visibility、COMMON/SPECIFIC 位相互独立。 */
enum class CfirDeclarationLinkage {
    WEAK_ODR, EXTERNAL, INTERNAL, LINKONCE_ODR, EXTERNAL_WEAK,
}

/**
 * linkage 分析之前已经发布的声明事实。
 *
 * 初始 EXTERNAL 对位原生 Decl 构造值；FFI、inline、反糖与 CJO 读取分别更新自己的事实，
 * 不从最终导出选择反推这些位。包图分析结果不回写此快照，声明复制只复制前序事实。
 */
data class CfirDeclarationExportFacts(
    val linkage: CfirDeclarationLinkage = CfirDeclarationLinkage.EXTERNAL,
    val isImplicitlyUsed: Boolean = false,
    val isInline: Boolean = false,
    val isImported: Boolean = false,
    val isGenericInstantiated: Boolean = false,
    val isInterfaceImplementation: Boolean = false,
    val isGlobal: Boolean = false,
    val sourceFactsPublished: Boolean = false,
    val isCompilerAdded: Boolean = false,
    /** 原生声明位，helper 的 HAS_INITIAL 与参数默认值位相互独立。 */
    val hasInitial: Boolean = false,
    val isImplicitlyAdded: Boolean = false,
    val hasNoReflectInfo: Boolean = false,
    /** 参数自己的 TOOL_ADD 位；包级 Imported/Foreign 和 COMPILER_ADD 都不能替代它。 */
    val isToolAdded: Boolean = false,
    val isDefaultParameterHelper: Boolean = false,
    val isCommonMatched: Boolean = false,
    /** 原生 INITIALIZED 位由正常 global/static 初始化分析或 CJO reader 发布，RAW 初值为 false。 */
    val isInitialized: Boolean = false,
    /** None 与 Empty() 都可有零 payload；原 LPAREN/type-list 存在性决定 VarInfo 或 FuncInfo。 */
    val enumConstructorHasParameterList: Boolean = false,
    /** 正式 CJO UNREACHABLE 协议省略语义签名；身份/header保留，不授予普通调用解析资格。 */
    val hasUnexportedSignature: Boolean = false,
)

/** 原始事实由声明 attributes 拥有，随同 attributes 复制且在阶段发布前更新。 */
internal object CfirDeclarationExportFactsKey : CfirDeclarationDataKey()

private var CfirDeclaration.exportFactsAttribute: CfirDeclarationExportFacts? by
    CfirDeclarationDataRegistry.data(CfirDeclarationExportFactsKey)

var CfirDeclaration.exportFacts: CfirDeclarationExportFacts
    get() = checkNotNull(exportFactsAttribute) { "Declaration attributes must contain their initial export facts" }
    set(value) {
        exportFactsAttribute = value
    }

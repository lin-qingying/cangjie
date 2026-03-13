package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

// ===== 符号相关 =====

/** 符号解析（对齐 KaResolver） */
interface CaResolver : CaLifetimeOwner

/** 符号关系：继承、实现、重写等（对齐 KaSymbolRelationProvider） */
interface CaSymbolRelationProvider : CaLifetimeOwner

/** 符号查询：按名称、ClassId 查找（对齐 KaSymbolProvider） */
interface CaSymbolProvider : CaLifetimeOwner

/** 符号详细信息（对齐 KaSymbolInformationProvider） */
interface CaSymbolInformationProvider : CaLifetimeOwner

// ===== 类型相关 =====

/** 类型查询（对齐 KaTypeProvider） */
interface CaTypeProvider : CaLifetimeOwner

/** 类型详细信息（对齐 KaTypeInformationProvider） */
interface CaTypeInformationProvider : CaLifetimeOwner

/** 类型关系：子类型、超类型检查（对齐 KaTypeRelationChecker） */
interface CaTypeRelationChecker : CaLifetimeOwner

/** 类型创建（对齐 KaTypeCreator） */
interface CaTypeCreator : CaLifetimeOwner

/** 类型替换器提供（对齐 KaSubstitutorProvider） */
interface CaSubstitutorProvider : CaLifetimeOwner

/** 签名替换（对齐 KaSignatureSubstitutor） */
interface CaSignatureSubstitutor : CaLifetimeOwner

// ===== 表达式相关 =====

/** 表达式类型推导（对齐 KaExpressionTypeProvider） */
interface CaExpressionTypeProvider : CaLifetimeOwner

/** 表达式信息（对齐 KaExpressionInformationProvider） */
interface CaExpressionInformationProvider : CaLifetimeOwner

/** 常量求值（对齐 KaEvaluator） */
interface CaEvaluator : CaLifetimeOwner

/** 数据流分析（对齐 KaDataFlowProvider） */
interface CaDataFlowProvider : CaLifetimeOwner

// ===== 作用域与诊断 =====

/** 诊断信息（对齐 KaDiagnosticProvider） */
interface CaDiagnosticProvider : CaLifetimeOwner {
    public fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>

    /**
     * Collects all diagnostics for the given file.
     */
    public fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>
}

/** 作用域查询（对齐 KaScopeProvider） */
interface CaScopeProvider : CaLifetimeOwner

/** 分析范围（对齐 KaAnalysisScopeProvider） */
interface CaAnalysisScopeProvider : CaLifetimeOwner

// ===== IDE 支持 =====

/** 代码补全候选检查（对齐 KaCompletionCandidateChecker） */
interface CaCompletionCandidateChecker : CaLifetimeOwner

/** 可见性检查（对齐 KaVisibilityChecker） */
interface CaVisibilityChecker : CaLifetimeOwner

/** 引用简化（对齐 KaReferenceShortener） */
interface CaReferenceShortener : CaLifetimeOwner

/** 导入优化（对齐 KaImportOptimizer） */
interface CaImportOptimizer : CaLifetimeOwner

/** 符号/类型渲染（对齐 KaRenderer） */
interface CaRenderer : CaLifetimeOwner

// ===== 源码与互操作 =====

/** 原始 PSI 访问（对齐 KaOriginalPsiProvider） */
interface CaOriginalPsiProvider : CaLifetimeOwner

/** 源代码查询（对齐 KaSourceProvider） */
interface CaSourceProvider : CaLifetimeOwner

/** C 互操作组件（对齐 KaJavaInteroperabilityComponent，仓颉用 C 互操作） */
interface CaCInteropComponent : CaLifetimeOwner

/** 文档注释查询（对齐 KaKDocProvider） */
interface CaDocProvider : CaLifetimeOwner

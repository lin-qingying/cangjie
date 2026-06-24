package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.types.ConeDiagnostic

/**
 * 只有文本原因和粗粒度分类的 cone 诊断。
 *
 * 该诊断用于错误类型、错误引用或临时恢复路径中尚未映射到结构化 checker 诊断的场景。
 *
 * @property reason 人类可读的诊断原因。
 * @property kind 诊断分类，供错误恢复和调试区分问题类型。
 */
class ConeSimpleDiagnostic(override val reason: String, val kind: DiagnosticKind = DiagnosticKind.Other) :
    ConeDiagnostic

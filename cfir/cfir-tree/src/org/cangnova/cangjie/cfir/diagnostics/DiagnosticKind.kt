package org.cangnova.cangjie.cfir.diagnostics

/**
 * 诊断分类，对齐 K2 `DiagnosticKind`。
 */
enum class DiagnosticKind {
    IllegalConstExpression,
    DeserializationError,
    InferenceError,
    RecursionInImplicitTypes,
    LoopInSupertype,
    DuplicateSupertype,
    ReturnNotAllowed,
    JumpOutsideLoop,
    NotLoopLabel,
    UnresolvedSupertype,
    CannotInferParameterType,
    EnumInitializerError,
    AmbiguousLabel,
    UnresolvedLabel,
    LabelNameClash,
    SuperNotAllowed,
    GenericTypeWithoutTypeArgument,
    Other,
}

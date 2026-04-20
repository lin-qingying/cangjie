// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
type MaybeInt = ?Int64
type NestedMaybeInt = ??Int64
type OptionalMapper = (?Int64) -> ?String

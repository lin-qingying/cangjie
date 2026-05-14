// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
type AA = Int64
type MaybeInt = ?Int64
type NestedMaybeInt = ??Int64
type HH = () -> Unit
type JJ = (Int64, Int64)
type KK = (Array<Int64>) -> Unit
type OptionalMapper = (?Int64) -> ?String

type Fixed = VArray<String, $4>

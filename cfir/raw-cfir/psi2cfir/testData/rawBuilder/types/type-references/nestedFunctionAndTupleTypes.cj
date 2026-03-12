// STUB_FALLBACK_TO_AST:
type Mapper = (Int64, String) -> Bool
type Pair = (Int64, String)
type HandlerTable = VArray<(Int64) -> Bool, $4>

type NestedMapper = ((Int64) -> String, Pair) -> Mapper
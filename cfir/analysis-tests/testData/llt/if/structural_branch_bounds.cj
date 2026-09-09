open class Root {}
class Left <: Root {}
class Right <: Root {}

let first = { x: (Any) -> Unit => () }
let second = { x: (Int64) -> Unit => () }
let argument = { x: Int64 => () }

func higherOrderIf(condition: Bool): Unit {
    let selected = if (condition) { first } else { second }
    <!NO_MATCHING_OPERATOR_INVOKE!>selected<!>(argument)
    let typed: ((Any) -> Unit) -> Unit = <!TYPE_MISMATCH!>selected<!>
}

func higherOrderMatch(condition: Bool): Unit {
    let selected = match (condition) {
        case true => first
        case false => second
    }
    <!NO_MATCHING_OPERATOR_INVOKE!>selected<!>(argument)
}

func higherOrderTry(): Unit {
    let selected = try { first } catch (_) { second }
    <!NO_MATCHING_OPERATOR_INVOKE!>selected<!>(argument)
}

func incomparableParameters(condition: Bool): Unit {
    let left = { x: Int64 => () }
    let right = { x: Bool => () }
    let selected = if (condition) { left } else { right }
    <!NO_MATCHING_OPERATOR_INVOKE!>selected<!>(1)
}

func primitiveReturns(condition: Bool): Unit {
    let left = {=> 1}
    let right = {=> true}
    let selected = if (condition) { left } else { right }
    <!NO_MATCHING_OPERATOR_INVOKE!>selected<!>()
}

func primitiveTupleJoin(condition: Bool): Unit {
    let selected = if (condition) { (1, true) } else { (true, 1) }
    let typed: (Any, Any) = <!TYPE_MISMATCH!>selected<!>
}

func classReturnJoin(condition: Bool): Root {
    let left = { x: Int64 => Left() }
    let right = { x: Int64 => Right() }
    let selected = if (condition) { left } else { right }
    selected(1)
}

func tupleParameterMeet(condition: Bool): Unit {
    let left = { x: (Left, Root) => () }
    let right = { x: (Root, Right) => () }
    let selected = if (condition) { left } else { right }
    let typed: ((Left, Right)) -> Unit = selected
    typed((Left(), Right()))
}

func functionParameterMeet(condition: Bool): Unit {
    let left = { f: (Left) -> Root => () }
    let right = { f: (Right) -> Root => () }
    let selected = if (condition) { left } else { right }
    let typed: ((Root) -> Root) -> Unit = selected
    typed({ x: Root => x })
}

func mixedFunctionAndTuple(condition: Bool): Unit {
    let selected = if (condition) { argument } else { (1, 2) }
    <!NO_MATCHING_OPERATOR_INVOKE!>selected<!>(1)
}

func unitIf(flag: Bool): Unit {
    if (flag) { 1 } else { "text" }
}
func unitMatch(flag: Bool): Unit {
    match (flag) { case true => 1; case false => "text" }
}
func nonTailMatch(flag: Bool) {
    match (flag) { case true => 1; case false => "text" }
    0
}
class DiscardedConstructor {
    init(flag: Bool) {
        if (flag) { 1 } else { "text" }
    }
}
func ignoredErrors(flag: Bool): Unit {
    if (flag) { <!UNRESOLVED_REFERENCE!>missingValue<!> } else { 1 }
}
func neededInitializer(flag: Bool): Unit {
    let value = <!TYPE_INCOMPATIBLE!>if (flag) { 1 } else { "text" }<!>
}
func neededLambdaBody(flag: Bool): Unit {
    let function = { => <!TYPE_INCOMPATIBLE!>if (flag) { 1 } else { "text" }<!> }
}
func discardedLambdaLiteral(flag: Bool): Unit {
    { => <!TYPE_INCOMPATIBLE!>if (flag) { 1 } else { "text" }<!> }
    0
}
func neededMatchLambda(flag: Bool): Unit {
    let function = { => <!TYPE_INCOMPATIBLE!>match (flag) { case true => 1; case false => "text" }<!> }
}
func neededReturnLambda(flag: Bool): Unit {
    let function = { => return <!TYPE_INCOMPATIBLE!>if (flag) { 1 } else { "text" }<!> }
}
func declaredLambdaResult(flag: Bool): Unit {
    let function: () -> Any = { => if (flag) { 1 } else { "text" } }
}
func inferredLambdaResult(flag: Bool): Unit {
    let function = { => if (flag) { 1 } else { 2 } }
    let value: Int64 = function()
}

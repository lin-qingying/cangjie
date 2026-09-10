public func matchBinding(value: Int64) {
    let result = match (value) {
        case bound => { => <!CANNOT_ASSIGN_TO_IMMUTABLE!>bound<!> = 1; bound }
    }
}
public func matchBranches(flag: Bool) {
    let result = match (flag) {
        case true => { => <!UNRESOLVED_REFERENCE!>missingMatch<!> }
        case false => { => 1 }
    }
}
public func ifBranches(flag: Bool) {
    let result = if (flag) { { => <!UNRESOLVED_REFERENCE!>missingIf<!> } } else { { => 1 } }
}
public func ifStatement(flag: Bool) {
    if (flag) { { => <!UNRESOLVED_REFERENCE!>missingStatement<!> } }
}
public func tryBranches() {
    let result = try { { => <!UNRESOLVED_REFERENCE!>missingTry<!> } } catch (_: Exception) { { => 1 } }
}

interface First {}
interface Second {}
class Left <: First & Second {}
class Right <: First & Second {}

func acceptInt(value: Int64): Unit {}
func acceptBool(value: Bool): Unit {}

func inferredResult(): Unit {
    let value = try { Left() } catch (_) <!MISMATCHING_CATCH_BLOCK!>{ Right() }<!>
}

func explicitResult(): Unit {
    let value: Any = try { Left() } catch (_) { Right() }
}

func exceptionalBranch(): Unit {
    let value = try { 1 } catch (_) { throw Exception() }
    acceptInt(value)
}

func discardedFinally(): Unit {
    let value = try { 1 } finally { "finally result" }
    acceptInt(value)
}

func erroneousFinally(): Unit {
    let value = try { 1 } finally { <!UNRESOLVED_REFERENCE!>missingFinally<!> }
    acceptBool(value)
}

func unknownCatch(): Unit {
    let value = try { 1 } catch (e: <!UNDECLARED_TYPE_NAME!>MissingException<!>) { e.foo(); 2 }
    acceptBool(value)
}

func invalidPattern(): Unit {
    let value = try { 1 } catch (_: <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>Nothing<!>) { 2 }
    acceptBool(value)
}

func nonTailTryError(): Unit {
    let value = try { <!UNRESOLVED_REFERENCE!>missingTry<!>; 1 } catch (_) { 2 }
    acceptBool(value)
}

func nonTailCatchError(): Unit {
    let value = try { 1 } catch (_) { <!UNRESOLVED_REFERENCE!>missingCatch<!>; 2 }
    acceptBool(value)
}

func nonTailFinallyError(): Unit {
    let value = try { 1 } finally { <!UNRESOLVED_REFERENCE!>missingFinally<!>; 2 }
    acceptBool(value)
}

func tryInInvokeArgument(): Unit {
    (try { <!UNRESOLVED_REFERENCE!>missingPipeline<!> } finally {}) |> {value: Unit => true}
    ({value: Int64 => true})(try { <!UNRESOLVED_REFERENCE!>missingInvoke<!>; 1 } finally {})
}

func localFunctionBody(): Unit {
    let value = try {
        func nested(): Unit { <!UNRESOLVED_REFERENCE!>missingFunctionBody<!> }
        1
    } catch (_) { 2 }
    acceptBool(<!ARGUMENT_TYPE_MISMATCH!>value<!>)
}

func localLambdaBody(): Unit {
    let value = try {
        let nested: () -> Unit = { => <!UNRESOLVED_REFERENCE!>missingLambdaBody<!> }
        1
    } catch (_) { 2 }
    acceptBool(value)
}

func localFunctionSignature(): Unit {
    let value = try {
        func nested(value: <!UNDECLARED_TYPE_NAME!>MissingParameter<!>): Unit {}
        1
    } catch (_) { 2 }
    acceptBool(value)
}

func localFunctionInUnsafe(): Unit {
    let value = unsafe {
        func nested(): Unit { <!UNRESOLVED_REFERENCE!>missingFunctionBody<!> }
        1
    }
    acceptBool(<!ARGUMENT_TYPE_MISMATCH!>value<!>)
}

func localLambdaInUnsafe(): Unit {
    let value = unsafe {
        let nested: () -> Unit = { => <!UNRESOLVED_REFERENCE!>missingLambdaBody<!> }
        1
    }
    acceptBool(value)
}

func localFunctionSignatureInUnsafe(): Unit {
    let value = unsafe {
        func nested(value: <!UNDECLARED_TYPE_NAME!>MissingParameter<!>): Unit {}
        1
    }
    acceptBool(value)
}

func validTrySelector(): Unit {
    match (try { (1, true) } catch (_: Exception) { (2, false) }) {
        case <!UNREACHABLE_PATTERN!>(_: Rune, _: Bool)<!> => ()
        case _ => ()
    }
}

func erroneousCatchSelector(): Unit {
    match (try { (1, true) } catch (_: Exception) {
        <!UNRESOLVED_REFERENCE!>missingCatch<!>
        (2, false)
    }) {
        case (_: Rune, _: Bool) => ()
        case _ => ()
    }
}

func erroneousFinallySelector(): Unit {
    match (try { (1, true) } finally { <!UNRESOLVED_REFERENCE!>missingFinally<!> }) {
        case (_: Rune, _: Bool) => ()
        case _ => ()
    }
}

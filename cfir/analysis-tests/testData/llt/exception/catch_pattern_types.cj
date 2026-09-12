open class CatchBase <: Exception {}
class CatchLeft <: CatchBase {}
class CatchRight <: CatchBase {}
type BottomAlias = Nothing

func acceptBase(value: CatchBase): Unit {}

func genericCatch<T>(): Unit where T <: Exception {
    try {} catch (e: T) {}
}

func aliasCatch(): Unit {
    try {} catch (_: <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>BottomAlias<!>) {}
}

func joinedCatch(): Unit {
    try {} catch (e: CatchLeft | CatchRight) { acceptBase(e) }
}

func mixedCatch(): Unit {
    try {} catch <!TYPE_INCOMPATIBLE!>(e: CatchLeft | Error)<!> { acceptBase(e); <!UNRESOLVED_REFERENCE!>missingBody<!> }
    try {} catch <!TYPE_INCOMPATIBLE!>(_: Exception | Error)<!> {}
}

func firstInvalidCatch(): Unit {
    try {} catch (_: <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>Nothing<!>) {} catch (_: Int64) { <!UNRESOLVED_REFERENCE!>missingLaterBody<!> }
}

func invalidAlternatives(): Unit {
    try {} catch (_: <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>Int64<!> | <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>Nothing<!>) {}
}

func unknownCatch(): Unit {
    try {} catch (e: <!UNDECLARED_TYPE_NAME!>MissingException<!>) { e.foo(); <!UNRESOLVED_REFERENCE!>missingBody<!> }
}

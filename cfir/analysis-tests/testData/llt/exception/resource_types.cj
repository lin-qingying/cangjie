class Bad {}
func acceptBool(value: Bool): Unit {}
class Good <: Resource {
    public func isClosed(): Bool { false }
    public func close(): Unit {}
}
func never(): Nothing { throw Exception() }

func erroneousResource(): Unit {
    try (<!TYPE_MISMATCH!>r = Bad()<!>) { r.missing(); <!UNRESOLVED_REFERENCE!>missingTry<!> }
    catch (_: Exception) { <!UNRESOLVED_REFERENCE!>missingCatch<!> }
    finally { <!UNRESOLVED_REFERENCE!>missingFinally<!> }
}

func bottomResource(): Unit {
    try (<!TYPE_MISMATCH!>r = never()<!>) {}
}

func resourceResult(): Unit {
    let value: Int64 = <!TYPE_MISMATCH!>try (r = Good()) { 1 } catch (_) { 2 }<!>
}

func validResource(): Unit {
    try (r = Good()) { r.isClosed() }
}

interface LeftFace {}
interface RightFace {}
class LeftResult <: LeftFace & RightFace {}
class RightResult <: LeftFace & RightFace {}

func resourceValueChecksBody(): Unit {
    let value: Unit = try (r = Good()) { <!TYPE_INCOMPATIBLE!>if (true) { LeftResult() } else { RightResult() }<!> }
}

func resourceBranchesDoNotJoin(): Unit {
    let value: Unit = try (r = Good()) { LeftResult() } catch (_) { RightResult() }
}

func protocolErrorResult(): Unit {
    let value = try (<!TYPE_MISMATCH!>r = Bad()<!>) {}
    acceptBool(<!ARGUMENT_TYPE_MISMATCH!>value<!>)
}

func resourceOrder(): Unit {
    try (<!TYPE_MISMATCH!>first = Bad()<!>, second = first.missing()) { <!UNRESOLVED_REFERENCE!>missingBody<!> }
}

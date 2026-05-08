// RUN_PIPELINE_TILL: FRONTEND
open class ParentEx <: Exception {}
class ChildEx <: ParentEx {}
class SiblingEx <: ParentEx {}

func parentThenChild(): Unit {
    try {
        throw ChildEx()
    } catch (e: ParentEx) {
        let _ = e
    }
    catch (e: <!USELESS_EXCEPTION_TYPE!>ChildEx<!>) {
        let _ = e
    }
}

func duplicateSibling(): Unit {
    try {
        throw SiblingEx()
    } catch (e: SiblingEx) {
        let _ = e
    }
    catch (e: <!USELESS_EXCEPTION_TYPE!>SiblingEx<!>) {
        let _ = e
    }
}

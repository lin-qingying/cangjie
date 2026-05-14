// RUN_PIPELINE_TILL: FRONTEND
open class ParentEx <: Exception {}
class ChildEx <: ParentEx {}

func unreachableCatch(): Unit {
    try {
        throw ChildEx()
    } catch (e: ParentEx) {
        let _ = e
    }
    catch (e: <!USELESS_EXCEPTION_TYPE!>ChildEx<!>) {
        let _ = e
    }
}

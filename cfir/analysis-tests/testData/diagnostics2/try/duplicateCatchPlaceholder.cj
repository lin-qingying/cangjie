// RUN_PIPELINE_TILL: FRONTEND
class DomainEx <: Exception {}

func duplicateCatch(): Unit {
    try {
        throw DomainEx()
    } catch (e: DomainEx) {
        let _ = e
    }
    catch (e: <!USELESS_EXCEPTION_TYPE!>DomainEx<!>) {
        let _ = e
    }
}

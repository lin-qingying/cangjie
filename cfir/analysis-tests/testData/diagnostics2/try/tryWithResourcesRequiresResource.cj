// RUN_PIPELINE_TILL: FRONTEND
class NotResource {}

class GoodResource <: Resource {
    public func isClosed(): Bool {
        return false
    }

    public func close(): Unit {}
}

func badResourceSpec(): Unit {
    try (<!MISMATCHED_TYPES_BECAUSE!>x = NotResource()<!>) {
        let _ = x
    }
}

func goodResourceSpec(): Unit {
    try (x = GoodResource()) {
        let _ = x
    }
}

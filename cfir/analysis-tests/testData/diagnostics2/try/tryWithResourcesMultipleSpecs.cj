// RUN_PIPELINE_TILL: FRONTEND
class GoodResourceA <: Resource {
    public func isClosed(): Bool {
        return false
    }

    public func close(): Unit {}
}

class GoodResourceB <: Resource {
    public func isClosed(): Bool {
        return false
    }

    public func close(): Unit {}
}

class BadResource {}

func mixedResources(): Unit {
    try (
        a = GoodResourceA(),
        <!MISMATCHED_TYPES_BECAUSE!>b = BadResource()<!>
    ) {
        let _ = a
        let _ = b
    }
}

func allGoodResources(): Unit {
    try (
        a = GoodResourceA(),
        b = GoodResourceB()
    ) {
        let _ = a
        let _ = b
    }
}

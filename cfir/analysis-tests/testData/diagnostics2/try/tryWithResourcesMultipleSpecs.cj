// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class GoodResourceA <: Resource {
    public func close(): Unit {}
}

class GoodResourceB <: Resource {
    public func close(): Unit {}
}

class BadResource {}

func mixedResources(): Unit {
    try (
        let a = GoodResourceA(),
        <!TRY_RESOURCE_MUST_IMPLEMENT_RESOURCE!>/* suggested message: every try-with-resources specification must implement Resource. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */let b = BadResource()<!>,
    ) {
        println(a)
        println(b)
    }
}

func allGoodResources(): Unit {
    try (
        let a = GoodResourceA(),
        let b = GoodResourceB(),
    ) {
        println(a)
        println(b)
    }
}

// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class NotResource {}

class GoodResource <: Resource {
    public func close(): Unit {}
}

func badResourceSpec(): Unit {
    try (<!TRY_RESOURCE_MUST_IMPLEMENT_RESOURCE!>/* suggested message: try-with-resources specification must implement Resource. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */let x = NotResource()<!>) {
        println(x)
    }
}

func goodResourceSpec(): Unit {
    try (let x = GoodResource()) {
        println(x)
    }
}

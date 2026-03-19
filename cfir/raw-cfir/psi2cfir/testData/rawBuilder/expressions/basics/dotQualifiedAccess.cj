// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
func testAccess(obj: MyClass): Unit {
    let v = obj.field
    obj.method(1)
}


// RUN_PIPELINE_TILL: FRONTEND

// FILE: objc_block.cj
package objc.lang

public class ObjCBlock<F> {
    private let callback: F
    public init(f: F) {
        callback = f
    }
    public prop call: F {
        get() {
            callback
        }
    }
}

public struct ObjCFunc<F> {}

// FILE: objc_call_property.cj
package objc.call

import objc.lang.*

main() {
    let block = ObjCBlock<(Int64) -> Int64>({ value => value })
    block.call(1)
    let escaped = <!OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED!>block.call<!>
    escaped
}

func rejectCFunc(value: ObjCFunc<<!OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE!>CFunc<() -> Unit><!>>) {}

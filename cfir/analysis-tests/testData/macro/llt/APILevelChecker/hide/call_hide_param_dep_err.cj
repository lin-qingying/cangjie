// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: call_hide_param_dep_err.cj



package ohos.dep

import ohos.labels.*

public class A {
    public A(@!Hide[isChecked: true] public var a!: Int64 = 1/* ok */, <!HIDE_AT_FUNC_PARAM!>@!Hide[isChecked: true] b!: Int64 = 2/* error */<!>) {}

    public init(<!HIDE_AT_FUNC_PARAM!>@!Hide[isChecked: true] b!: String<!>) { a = 2 }

    public func foo(<!HIDE_AT_FUNC_PARAM!>@!Hide[isChecked: true] c: Int64<!>) {}
}

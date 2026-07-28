// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: call_hide_param_dep.cj


package ohos.dep

import ohos.labels.*

public class A {
    public A(@!Hide[isChecked: true] public var a!: Int64 = 1) {}
}

// FILE: call_hide_param.cj



import ohos.dep.*

main() {
    <!UNRESOLVED_REFERENCE!>A().a<!>
}

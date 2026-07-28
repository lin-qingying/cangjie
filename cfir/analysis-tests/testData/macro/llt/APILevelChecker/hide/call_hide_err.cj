// FILE: hide_err.cj


package ohos.labels1111

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_use_err.cj


package ohos.dep

import ohos.labels1111.*

@!Hide[isChecked: true]
public class A {}

// FILE: call_hide_err.cj



import ohos.dep.*

main() {
    var a = A()
    0
}

// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_in_extend3.cj



package ohos.dep

import ohos.labels.*

public class A {}

@!Hide[isChecked: true]
extend A {}

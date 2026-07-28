// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: multi_hide.cj



package ohos.dep

import ohos.labels.*

@!Hide
@!Hide[isChecked: true]
public <!HIDE_MULTI_ANNOTATION!>class A<!> {}

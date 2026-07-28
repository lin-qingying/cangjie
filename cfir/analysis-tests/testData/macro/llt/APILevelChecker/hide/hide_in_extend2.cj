// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_in_extend2.cj



package ohos.dep

import ohos.labels.*

@!Hide[isChecked: true]
public class A {}

<!HIDE_DIFF_PARAM!>@!Hide[isChecked: false]
extend A {}<!>

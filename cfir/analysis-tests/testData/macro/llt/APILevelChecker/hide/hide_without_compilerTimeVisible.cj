// FILE: hide.cj


package ohos.labels

@Annotation
protected class Hide {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_without_compilerTimeVisible.cj



package ohos.dep

import ohos.labels.*

@<!HIDE_COMPILE_TIME_INVISIBLE!>Hide[isChecked: true]<!>
public class A {}

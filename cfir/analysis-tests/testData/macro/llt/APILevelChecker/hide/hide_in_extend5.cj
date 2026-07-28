// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_in_extend5.cj



package ohos.dep

import ohos.labels.*

public class A {}

@!Hide[isChecked: false]
extend A {
    @!Hide
    public func foo() {}
    @!Hide[isChecked: false]
    public func goo() {}
    <!HIDE_DIFF_PARAM!>@!Hide[isChecked: true] // error
    public func hoo() {}<!>
    public func ioo() {}
}

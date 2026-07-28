// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_in_extend6.cj



package ohos.dep

import ohos.labels.*

public class A {}

@!Hide[isChecked: true]
extend A {
    <!HIDE_DIFF_PARAM!>@!Hide
    public func foo() {} // error<!>
    <!HIDE_DIFF_PARAM!>@!Hide[isChecked: false]
    public func goo() {} // error<!>
    @!Hide[isChecked: true]
    public func hoo() {}
    public func ioo() {}
}

// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: override_by_hide1.cj



package ohos.dep

import ohos.labels.*

public interface I {
    func <!HIDE_MISSING_HIDE!>foo<!>() {}
    func <!HIDE_MISSING_HIDE!>goo<!>() {}
    func <!HIDE_MISSING_HIDE!>koo<!>() {}
}

public class A <: I {
    @!Hide
    public func foo() {}
    @!Hide[isChecked: false]
    public func goo() {}
    @!Hide[isChecked: true]
    public func koo() {}
}

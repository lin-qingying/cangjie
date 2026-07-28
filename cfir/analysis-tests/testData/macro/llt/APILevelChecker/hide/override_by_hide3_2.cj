// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: override_by_hide3_2.cj



package ohos.dep

import ohos.labels.*

public interface I {
    func <!HIDE_MISSING_HIDE!>foo<!>() {}
    func <!HIDE_MISSING_HIDE!>goo<!>() {}
    func <!HIDE_MISSING_HIDE!>koo<!>() {}
}

@!Hide[isChecked: false]
public class B <: I {
    public func foo() {}
    public func goo() {}
    public func koo() {}
}

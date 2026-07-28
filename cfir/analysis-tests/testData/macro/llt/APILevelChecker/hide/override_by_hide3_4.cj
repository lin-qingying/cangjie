// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: override_by_hide3_4.cj



package ohos.dep

import ohos.labels.*

@!Hide[isChecked: true]
public interface I {
    func foo() {}
    func goo() {}
    func koo() {}
}

@!Hide[isChecked: true]
public class C <: I {
    public func foo() {}
    public func goo() {}
    public func koo() {}
}

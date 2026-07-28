// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: override_by_hide6_2.cj



package ohos.dep

import ohos.labels.*

public interface I1 {
    func <!HIDE_MISSING_HIDE!>foo<!>() {}
}

@!Hide
public class A1 <: I1 {
    public func foo() {}
}

@!Hide
public interface I2 {
    func foo() {}
}

public class A2 <: I2 {
    public func foo() {}
}

@!Hide[isChecked: false]
public interface I3 {
    func foo() {}
}

@!Hide[isChecked: true]
public class A3 <: I3 {
    public func <!HIDE_DIFF_PARAM!>foo<!>() {}
}

@!Hide[isChecked: true]
public interface I4 {
    func foo() {}
}

@!Hide[isChecked: false]
public class A4 <: I4 {
    public func <!HIDE_DIFF_PARAM!>foo<!>() {}
}

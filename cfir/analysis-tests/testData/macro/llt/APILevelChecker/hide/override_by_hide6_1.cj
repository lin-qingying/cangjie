// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: override_by_hide6_1.cj



package ohos.dep

import ohos.labels.*

@!Hide
public interface I1 {
    func foo() {}
}

@!Hide
public class A1 <: I1 {
    public func foo() {}
}

@!Hide[isChecked: true]
public interface I2 {
    func foo() {}
}

@!Hide[isChecked: true]
public class A2 <: I2 {
    public func foo() {}
}

@!Hide
public interface I3 {
    func foo() {}
}

@!Hide[isChecked: false]
public class A3 <: I3 {
    public func foo() {}
}

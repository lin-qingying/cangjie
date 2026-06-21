// FILE: hide.cj


package ohos.labels

@Annotation
protected class Hide {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: call_hide_extend_dep.cj


package ohos.dep

import ohos.labels.*

public class A {
    public func foo() {}
}

@!Hide[isChecked: true]
extend A {
    public func goo() {}
}

extend A {
    @!Hide[isChecked: true]
    public func koo() {}
}

// FILE: hide_in_extend4.cj



package ohos.dep

import ohos.labels.*

public class A {}

@!Hide[isChecked: false]
extend A {}

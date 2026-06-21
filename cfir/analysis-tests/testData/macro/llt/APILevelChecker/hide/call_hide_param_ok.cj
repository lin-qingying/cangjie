// FILE: hide.cj


package ohos.labels

@Annotation
protected class Hide {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: call_hide_param_dep.cj


package ohos.dep

import ohos.labels.*

public class A {
    public A(@!Hide[isChecked: true] public var a!: Int64 = 1) {}
}

// FILE: call_hide_param_ok.cj



package ohos.default

import ohos.dep.*

main() {
    A().a
}

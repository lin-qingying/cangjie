

class A<T> {
    mut prop a: String {
        get() {
            b
        }
        set(v) {
            b = v
        }
    }
    var b: String = "a${<!USED_BEFORE_INITIALIZATION!>a<!>}"
}

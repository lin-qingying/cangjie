

class A<T> {
    mut prop a: String {
        get() {
            b
        }
        set(v) {
            b = v
        }
    }
    var b: String = "a${<!ILLEGAL_USAGE_OF_MEMBER!>a<!>}"
}

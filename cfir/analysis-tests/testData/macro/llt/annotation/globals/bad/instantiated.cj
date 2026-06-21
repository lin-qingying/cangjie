

@Annotation[target: [GlobalFunction]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

class S<T> {
    <!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[1]<!>
    func foo() {}

    <!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[1]<!>
    var c = 3
}

enum E<T> {
    | <!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[1]<!>
    My(Int32)

    <!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[1]<!>
    static func foo<R>() {}
}

main() {
    S<Int32>().foo()
    S<Int64>().foo()
    E<Int32>.foo<Int32>()
    E<Int64>.foo<String>()
}

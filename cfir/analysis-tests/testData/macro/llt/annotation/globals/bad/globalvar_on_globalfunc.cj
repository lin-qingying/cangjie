


@Annotation[target: [GlobalVariable]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[11]<!>
func foo() {}

@Annotation[target: [MemberFunction]]
public class APMLevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@APMLevel[11]<!>
func bar() {}

class M {
    <!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[3]<!>
    func bar() {}
    <!ANNOTATION_NOT_APPLICABLE_JFFI!>@APMLevel[3]<!>
    var t = 3
}

main() {
    foo()
    bar()
    M().bar()
    0
}



@Annotation
public class A {
    const init() {}
}

@A
class B {
    func f() {
        <!ANNOTATION_CUSTOM_PLACE!>@A<!>
        func g() {}
        g()
    }
}

main() {
    B().f()
}

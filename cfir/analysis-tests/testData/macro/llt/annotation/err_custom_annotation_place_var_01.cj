

@Annotation
public class A {
    const init() {}
}

class B {
    func f() {
        <!ANNOTATION_CUSTOM_PLACE!>@A<!>
        let x = 0
        x
    }
}

main() {}

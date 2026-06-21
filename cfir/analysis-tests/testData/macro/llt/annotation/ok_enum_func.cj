

@Annotation
public class A {
    const init() {}
}

enum E {
    A

    @A
    func f() {}
}

main() {
    E.A.f()
}

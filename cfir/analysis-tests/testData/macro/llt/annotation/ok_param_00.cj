

@Annotation
public class A {
    const init() {}
}

class B {
    func f(@A x: Int64) {
        x
    }
}

main() {
    B().f(0)
}

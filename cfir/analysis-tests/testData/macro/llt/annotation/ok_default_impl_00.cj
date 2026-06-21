

@Annotation
public class A {
    const init() {}
}

struct S <: I {}

interface I {
    @A
    func f() {}
}

main() {}

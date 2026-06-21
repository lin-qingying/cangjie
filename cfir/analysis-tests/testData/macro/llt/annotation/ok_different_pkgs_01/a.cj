// FILE: b.cj


package pkg

@Annotation
public class A {
    public const init() {}
}

// FILE: a.cj



internal import pkg.A

@A
class B {}

main() {}

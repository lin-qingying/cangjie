// FILE: a.cj


package pkg

@Annotation
public class A {
    public const init() {}
}

// FILE: b.cj



internal import pkg.A

@A
class B {}

main() {}

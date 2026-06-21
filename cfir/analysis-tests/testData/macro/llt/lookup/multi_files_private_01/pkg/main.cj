// FILE: pkg.cj


package example

public class XX {
    const init() {}
    static const ALL = XX()
}

private type example_XX = XX

// FILE: main.cj


package example

@Annotation
public class A {
    public const init(p0!: <!UNRESOLVED_REFERENCE!>example_XX<!> = <!UNRESOLVED_REFERENCE!>example_XX<!>.ALL) {}
}

main() {}

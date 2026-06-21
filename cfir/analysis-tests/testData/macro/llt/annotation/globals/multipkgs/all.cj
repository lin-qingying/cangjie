// FILE: all.pkg.cj


package a

@Annotation
public class F {
    public const init() {}
}

// FILE: all.cj



import a.*

@F
extend Int8 {}

@F
func foo() {}

@F
public var a = 3

main() {}

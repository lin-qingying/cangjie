// FILE: 255.pkg.cj


package a

@Annotation[target: [Type, Parameter, Init, MemberProperty, MemberFunction, MemberVariable, EnumConstructor,
    GlobalFunction]]
public class F {
    public const init() {}
}

// FILE: 255.cj



import a.*

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@F<!>
extend Int8 {}

@F
func foo() {}

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@F<!>
public var a = 3

main() {
    foo()
    0
}

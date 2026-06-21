

@Annotation
public class A { const init(_: Int64) {} }

@A[{ =>
    <!ANNOTATION_CUSTOM_PLACE!>@A[0]<!> let x = 0
    x
}()]
class B {}

main() {}

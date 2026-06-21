

@Annotation[target: [Extension]]
public class A {
    const init() {}
}

@A
extend Int8 {
    <!ANNOTATION_NOT_APPLICABLE_JFFI!>@A<!>
    func foo<T>() {}
}

main() {
    let a = Int8(3)
    a.foo<Int32>()
    a.foo<Int>()
    a.foo<UIntNative>()
}

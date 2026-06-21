

@Annotation
public class A {
    const init() {}
    const init(_: Int64) {}
}

@A
@A[0]
class B {}

main() {}

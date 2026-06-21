

@Annotation
public class A<T> {
    public const A(let data: T) {}
}

@A[0]
@A["zero"]
class B {}

main() {}

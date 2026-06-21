

@Annotation
public class A {
    const init() {}
}

type AA = A

@A
@AA
class B {}

main() {}

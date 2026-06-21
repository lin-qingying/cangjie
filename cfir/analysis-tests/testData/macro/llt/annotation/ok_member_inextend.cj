

@Annotation
public class CustomAnnotation {
    let name: String
    let version: Int64

    public const init(name: String, version!: Int64 = 0) {
        this.name = name
        this.version = version
    }
}

public class TestClass {
    public const init() {}
}

@Annotation
public class A {
    const init() {}
}

extend TestClass {
    @CustomAnnotation["Sample", version: 1]
    public static prop prop1: Int64 {
        get() {
            1
        }
    }

    @CustomAnnotation["Sample", version: 1]
    public prop prop2: Int64 {
        get() {
            1
        }
    }
    @CustomAnnotation["Sample", version: 1]
    public func f1() {
        1
    }
    @CustomAnnotation["Sample", version: 1]
    public static func f2() {
        1
    }
    public func f(@A x: Int64) {
        x
    }
}

main() {
    0
}

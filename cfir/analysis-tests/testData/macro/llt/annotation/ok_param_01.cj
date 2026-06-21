


internal import std.reflect.*

@Annotation
public class A {
    const init() {}
}

public class B {
    public func f(@A x: Int64) {
        x
    }
}

main() {
    let v = TypeInfo.of<B>().getInstanceFunction("f", TypeInfo.of<Int64>()).parameters[0].annotations.size
    if (v == 1) {
        return 0
    }
    return 1
}

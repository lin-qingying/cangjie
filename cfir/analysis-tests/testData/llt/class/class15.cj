

public abstract class A {
    public func a(): Int64
}

public abstract class B <: A {
    public func c(): Int64
    public func b(): Int64 {
        0
    }
}

public class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C<!> <: B {
    public func c(): Int64 {
        3
    }
}

main() {
    var c = C()
    if (c.c() == 3 && c.b() == 4) {
        print("Pass\n")
    }
    return 0
}

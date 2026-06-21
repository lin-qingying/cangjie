


public interface I1 {
    func f() {}
}

public interface I2 {
    func f() {}
}

public open class C1 <: I2 {}

public class C2 <: C1 & I1 {
    public func f() {}
}

main() {
    return 0
}

extend C1 {}

extend C2 {}

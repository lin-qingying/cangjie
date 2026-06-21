// FILE: pkgs/pkgs.cj


package pkgs

public interface I1 {
    func foo(): Int64
}

public interface I2 {
    func foo(): Int64 {
        return 1
    }
}

public open class C1 {
    public open func foo(): Int64 {
        return 2
    }
}

public open class C2 <: C1 & I1 & I2 {
    public override func foo(): Int64 {
        print("calling foo() in C2\n")
        return 3
    }
}

public class C3 <: C2 {}

// FILE: main.cj



internal import pkgs.*

main(): Int64 {
    var res = C3().foo()
    if (res == 3) {
        return 0
    }
    return 1
}

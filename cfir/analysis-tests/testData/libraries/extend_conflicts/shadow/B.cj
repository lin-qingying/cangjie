package B

internal import A.Foo

public interface I1 {
    func f(): Unit
}

extend Foo <: I1 {
    public func f(): Unit {
        print("I1.f\n")
    }
}

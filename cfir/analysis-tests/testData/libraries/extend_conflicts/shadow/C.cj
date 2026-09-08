package C

internal import A.Foo

public interface I2 {
    func f(): Unit
}

extend Foo <: I2 {
    public func f(): Unit {
        print("I2.f\n")
    }
}



public interface fatherInterface {
    func foo1(): Int64
    prop b: Int64 {
        get() {
            foo1()
        }
    }
}

public interface MyInterface <: fatherInterface {
    prop b: Int64 {
        get() {
            3
        }
    }
}

public enum myEnum {
    A
}

extend myEnum <: MyInterface {
    public func foo1(): Int64 {
        1
    }
}

main() {
    var enum3: myEnum = myEnum.A
    return enum3.b - 3
}

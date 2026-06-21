


abstract class Base {
    public static func get(): Int32 {
        return 1
    }
}

class Data <: Base {}

func run(): Int64 {
    var value: Data = Data()
    if (<!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>v<!>alue.get() != 1) {
        return 1
    }
    return 0
}

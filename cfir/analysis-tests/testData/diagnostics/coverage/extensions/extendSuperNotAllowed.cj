// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    func value(): Int64 {
        return 1
    }
}

class Host <: Base {}

extend Host {
    func read(): Int64 {
        return <!EXTEND_SUPER_NOT_ALLOWED!>super<!>.value()
    }
}

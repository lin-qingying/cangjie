public open class Father {}
public class Son <: Father {}
public class Uncle {}
public enum Family { son(Son) }
public enum OpenFamily { father(Father) }
public func knownEnum(): Int64 {
    let value = Family.son(Son())
    match (value) {
        case <!UNREACHABLE_PATTERN!>son(_: Uncle)<!> => 0
        case <!UNREACHABLE_PATTERN!>son(_: Father)<!> => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
public func unknownEnum(value: Family): Int64 {
    match (value) {
        case <!UNREACHABLE_PATTERN!>son(_: Uncle)<!> => 0
        case son(_: Father) => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
public func openEnum(value: OpenFamily): Int64 {
    match (value) {
        case father(_: Son) => 0
        case _ => 1
    }
}
public func knownSubtype(value: Son): Int64 {
    match (value) {
        case _: Father => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
}
public func genericEnum<T>(value: Option<T>): Int64 {
    match (value) {
        case Some(_: Int64) => 0
        case _ => 1
    }
}

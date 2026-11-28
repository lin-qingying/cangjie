


interface I {
    static func f(): Int64
}

struct C {}

<!NEED_MEMBER_IMPLEMENTATION!>extend C <: I {}<!>

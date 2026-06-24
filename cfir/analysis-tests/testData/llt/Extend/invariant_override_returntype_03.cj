


public interface I {
    func <!RETURN_TYPE_INVARIANCE!>append<!>(_: String): Any
}

extend StringBuilder <: I {}

main() {}

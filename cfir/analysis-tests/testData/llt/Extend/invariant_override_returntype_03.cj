


public interface I {
    func append(_: String): Any
}

extend StringBuilder <: I {}

main() {}

// STUB_FALLBACK_TO_AST:
public interface Display {}
public interface HashableLike {}

public enum Wrapper<T> {
    | Item(T)
}

extend<T> Wrapper<T> <: Display where T <: Display {}
extend<T> Wrapper<T> <: HashableLike where T <: HashableLike {}
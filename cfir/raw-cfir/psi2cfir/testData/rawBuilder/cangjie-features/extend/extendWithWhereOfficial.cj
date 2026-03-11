// STUB_FALLBACK_TO_AST:
public enum EnumExtend<T> {
    | AA
}

extend<T> EnumExtend<T> <: Equatable<EnumExtend<T>> where T <: Equatable<T> {}
extend<T> EnumExtend<T> <: ToString where T <: ToString {}
extend<T> EnumExtend<T> <: Hashable where T <: Hashable {}
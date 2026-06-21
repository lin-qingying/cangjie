

public interface Cloneable<T> {}

public interface MockCommonClientInterface<T, TList> where T <: Cloneable<T>, TList <: Cloneable<TList> {}

func applyFor<T, TList>(owner!: MockCommonClientInterface<T, TList>, subresource!: String = ""): Unit where T <: Cloneable<T>,
    TList <: Cloneable<TList> {
}

main() {}

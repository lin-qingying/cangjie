interface Holder<T> {}
interface Face {}
class Derived <: Face {}

func useFunction<A, B>(function: (A) -> B): Unit where A <: Holder<B> {}

func uses(): Unit {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>useFunction<!>({value: Holder<Face> => Derived()})
    useFunction<Holder<Face>, Face>({value: Holder<Face> => Derived()})
    useFunction({value: Holder<Derived> => Derived()})
}

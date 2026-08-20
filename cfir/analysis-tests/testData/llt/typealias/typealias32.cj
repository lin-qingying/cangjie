

class GennericClassA<T> {}

type TypeClass<Y> = GennericClassA<Y>

type TypeB<X> = TypeClass<X>

type TypeC = TypeClass<Int64>

main() {
    var a = <!UNABLE_TO_INFER_GENERIC_FUNC!>TypeB<!>()
    var b = <!UNABLE_TO_INFER_GENERIC_FUNC!>TypeClass<!>()
    var c = <!UNABLE_TO_INFER_GENERIC_FUNC!>GennericClassA<!>()
    var d = TypeC()
    0
}

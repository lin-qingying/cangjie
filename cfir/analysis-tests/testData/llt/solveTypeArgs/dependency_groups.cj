interface Self<T> {}
interface PairRel<A, B> {}
interface Other<T> {}

class Left <: Self<Left> & PairRel<Left, Right> & Other<Right> {}
class Right <: Self<Right> & PairRel<Right, Left> & Other<Left> {}

func self<T>(value: T): Unit where T <: Self<T> {}
func independent<A, B>(a: A, b: B): Unit where A <: Self<A>, B <: Self<B> {}
func oneWay<A, B>(a: A, b: B): Unit where A <: PairRel<A, B> {}
func simpleCycle<A, B>(a: A, b: B): Unit where A <: Other<B>, B <: Other<A> {}
func coupled<A, B>(a: A, b: B): Unit where A <: PairRel<A, B>, B <: PairRel<B, A> {}

func uses(): Unit {
    self(Left())
    independent(Left(), Right())
    oneWay(Left(), Right())
    simpleCycle(Left(), Right())
    <!UNABLE_TO_INFER_GENERIC_FUNC!>coupled<!>(Left(), Right())
    coupled<Left, Right>(Left(), Right())
}

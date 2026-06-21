

enum List<A> {
    Nil | Cons(A, List<A>)
}

func fold<A, B>(f: (A) -> (B) -> B): (B) -> (List<A>) -> B {
    {
        e: B => {
            l: List<A> => match (l) {
                case Nil => e
                case Cons(x, xs) => fold(f)(e)(xs) |> f(x)
            }
        }
    }
}

func map<A, B>(f: (A) -> B): (List<A>) -> List<B> {
    {l: List<A> => l |> fold {x: A => {xs: List<B> => Cons(f(x), xs)}}(Nil)}
}

func listSum(l: List<Int64>) {
    l |> (0 |> fold<Int64, Int64> {x => {xs => x + xs}})
}

func listProduct(l: List<Int64>) {
    l |> (1 |> fold<Int64, Int64> {x => {xs => x * xs}})
}

func mulThree(l: List<Int64>) {
    l |> map({x: Int64 => x * 3})
}

let testCase = Cons(1, Cons(-10, Cons(100, Nil)))

main() {
    let x = testCase |> (mulThree ~> listSum)
    let y = testCase |> (mulThree ~> listProduct)
    println(x)
    println(y)
    0
}

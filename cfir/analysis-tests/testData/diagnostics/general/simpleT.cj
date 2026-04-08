
class A<T>{

    func abc(a:T):Unit{}
}
struct DefaultOps1<T> {
    func stableId(): Int64 {
        return 1
    }

    func echo(v: T): Unit{}
}
enum DefaultOps2<T> {
    Some(T) | None

    func echo(v: T): Unit{}
}
interface DefaultOps<T> {
    func stableId(): Int64 {
        return 1
    }

    func echo(v: T): Unit{}
}
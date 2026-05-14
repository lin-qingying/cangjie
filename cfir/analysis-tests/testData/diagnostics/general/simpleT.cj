
class A<T>{

    func abc(a:T):Unit{
        let _ = a
    }
}
struct DefaultOps1<T> {
    func stableId(): Int64 {
        return 1
    }

    func echo(v: T): Unit{
        let _ = v
    }
}
enum DefaultOps2<T> {
    Some(T) | None

    func echo(v: T): Unit{
        let _ = v
    }
}
interface DefaultOps<T> {
    func stableId(): Int64 {
        return 1
    }

    func echo(v: T): Unit{
        let _ = v
    }
}

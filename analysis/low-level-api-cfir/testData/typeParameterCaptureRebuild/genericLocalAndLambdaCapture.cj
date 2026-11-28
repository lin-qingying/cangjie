// local 自有 U，lambda 无自有泛型；两者捕获原 Owner<T> self。M 不期待语义诊断。
package lowlevel.typeparametercapture
class Owner<T> {
    func read<R>(value: T, other: R): T {
        func local<U>(outer: T, own: U): T { return @M(outer) }
        invoke({ item: T => return @M(item) }, value)
        return @M(value)
    }
}
func invoke<T>(action: (T) -> T, value: T): T { return action(value) }

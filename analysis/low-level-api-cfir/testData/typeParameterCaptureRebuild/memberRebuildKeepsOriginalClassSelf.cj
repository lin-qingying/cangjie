// designation 祖先是实际旧 Owner<T>；函数自己的 R 属于 fresh header。
// M 未声明，不期待语义诊断。
package lowlevel.typeparametercapture
class Owner<T> {
    func read<R>(value: T, other: R): T { return @M(value) }
}

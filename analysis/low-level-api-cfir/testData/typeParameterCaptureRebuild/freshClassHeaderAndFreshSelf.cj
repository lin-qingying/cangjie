// 完整 fresh nominal 的 self 使用自己的 fresh T；M 不期待语义诊断。
package lowlevel.typeparametercapture
class Owner<T> {
    public Owner() {}
    func read(value: T): T { return @M(value) }
}

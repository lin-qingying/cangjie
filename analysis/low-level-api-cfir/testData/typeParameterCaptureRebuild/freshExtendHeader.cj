// extend 只声明自己的 R，不借用目标 Box 的 T；M 不期待语义诊断。
package lowlevel.typeparametercapture
class Box<T> {}
extend<R> Box<R> {
    func read(value: R): R { return @M(value) }
}

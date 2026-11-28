// 构造器不声明自有泛型，使用 constructed class 的 T。
package lowlevel.typeparametercapture
class Owner<T> {
    public Owner(value: T) {}
    public init(value: T, count: Int64) {}
}

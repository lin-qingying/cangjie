// 构造器不声明自有泛型，使用 constructed struct 的 T。
package lowlevel.typeparametercapture
struct Owner<T> {
    public Owner(value: T) {}
    public init(value: T, count: Int64) {}
}

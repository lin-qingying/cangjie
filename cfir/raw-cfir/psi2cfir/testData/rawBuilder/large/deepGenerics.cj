// RUN_PIPELINE_TILL: FRONTEND
// 大文件场景：12 层嵌套的泛型实参（构造链 12 层，Wrap 与 Box 各 8 组）、类嵌套 / 结构嵌套的显式类型实参（3~4 层）、
// 单类型形参与双 / 三类型形参的 where 约束（where 写在返回类型之后，约束用 `T <: Bound`），用于覆盖构树时泛型实参列表、
// 类型实参列表与 where 约束节点。注：12 层构造链配显式类型实参时官方约束求解会失败（sema_unable_to_infer_generic_func），
// 故长链只写构造；cjc 1.0.5 / 1.1.3 零诊断。
class Box0<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap0<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box1<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap1<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box2<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap2<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box3<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap3<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box4<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap4<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box5<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap5<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box6<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap6<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box7<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap7<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box8<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap8<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box9<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap9<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box10<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap10<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Box11<T0, T1> {
    var a: T0
    var b: T1

    public init(a: T0, b: T1) { this.a = a; this.b = b }
}
class Wrap11<T0, T1> {
    var inner: T0
    var other: T1

    public init(a: T0, b: T1) { inner = a; other = b }
}
class Leaf {
    public init() {}
}
func deepGenericArgs(): Unit {
    let _ = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
}

func deepGenericTypes(): Unit {
    let a1 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b1 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let a2 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b2 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let a3 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b3 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let a4 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b4 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let a5 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b5 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let a6 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b6 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let a7 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b7 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let a8 = Wrap11(11, Wrap10(10, Wrap9(9, Wrap8(8, Wrap7(7, Wrap6(6, Wrap5(5, Wrap4(4, Wrap3(3, Wrap2(2, Wrap1(1, Wrap0(0, Leaf()))))))))))))
    let b8 = Box11(11, Box10(10, Box9(9, Box8(8, Box7(7, Box6(6, Box5(5, Box4(4, Box3(3, Box2(2, Box1(1, Box0(0, Leaf()))))))))))))
    let leaf: Leaf = Leaf()
    let c = Wrap2(Wrap1(Wrap0(1, 2), 3), 4)
    let d: Box0<Box1<Box2<Int64, Int64>, Int64>, Int64> = Box0(Box1(Box2(1, 2), 3), 4)
    let e: Wrap2<Wrap1<Wrap0<Box1<Int64, Int64>, Int64>, Int64>, Int64> = Wrap2(Wrap1(Wrap0(Box1(1, 2), 3), 4), 5)

}
func deepGenericWhere<T, U>(a: T, b: U): T where T <: ToString, U <: Hashable {
    return a
}

func deepGenericWhereChain<T, U, V>(a: T, b: U, c: V): V where T <: ToString, U <: Hashable, V <: Comparable<V> {
    return c
}

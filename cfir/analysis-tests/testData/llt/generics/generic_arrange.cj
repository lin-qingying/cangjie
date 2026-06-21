

import std.reflect.ClassTypeInfo
import std.reflect.TypeInfo

public func getClazzName1<T>() {
    return (ClassTypeInfo.of<T>() as ClassTypeInfo).getOrThrow().name
}

public class C1 {}

public class C2 {}

main() {
    let c1 = getClazzName1<C1>() // line1
    let c2 = getClazzName1<C2>() // line2
    println("c1 = ${c1}") // line3    // c1 = C2   // 异常
    println("c2 = ${c2}") // line4    // c1 = C2    // 正确
}

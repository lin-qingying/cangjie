// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs0018002a

internal import pkgs0018001a.*

public func NormalFunc(a: Int32): Int32 {
    return a
}

public func Foo<T, U>(a: T, b: U): T {
    return a
}

public func Bar<T>(a: T) {
    var b: Int32 = 1
    var c = Foo<T, Int32>(a, b)
    return c
}

public struct Test<T> {
    public let member1: Int32
    public var member2: T
    public init(a: Int32, b: T) {
        member1 = a
        member2 = b
    }
    public func testNormal(a: Bool): Bool {
        return a
    }
    public func testGeneric1(t: T): T {
        return t
    }
    public func testGeneric2(t: T, j: Bool): Bool {
        return j
    }
}

public struct R1<A> where A <: Add<A> {
    public R1(let a: A) {}
    public func test(): A {
        return a
    }
    public func test1(t: A): A {
        return t
    }
}

public struct R2<B> where B <: Add<B> {
    public let a: B
    public init(x: Int32, b: B) {
        a = b
    }
    public init(b: B) {
        a = b
    }
}

public func run1(): Int32 {
    var obj1: C1<Int32> = C1<Int32>(0)
    obj1.a = 1
    var ans1 = obj1.a + obj1.test() * 10 + obj1.test1(100) // 111
    var obj2 = C2<Int32>(0, 1000)
    var obj3 = C2<Int32>(10000)
    var obj4 = C2<Int64>(100000)
    var ans2 = obj2.a + obj3.a + Int32(obj4.a) + ans1 // 111000 + 111 = 111111

    var value = Test<Int32>(1, 10)
    var b = value.member1 + value.member2
    var c = value.testNormal(false)
    var d = value.testGeneric1(123)
    var e = value.testGeneric2(123, true)

    var f: R1<Int32> = R1<Int32>(0)
    let f_fake_test = f.a + 1
    var g = f_fake_test + f.test1(10) // 11
    var h = R1<Int32>(100)
    var i = R1<Int64>(1000)
    var j = h.a + Int32(i.test()) // 1100
    var k = R2<Int32>(0, 0)
    var l = R2<Int32>(10000)
    var m = k.a + l.a // 10000
    var res = g + j + m

    var n: Int32 = 3
    var o = NormalFunc(n)
    var p: Int32 = 4
    var q = Bar<Int32>(p)

    if (ans2 == 111111 && value.member1 == 1 && value.member2 == 10 && b == 11 && c == false && d == 123 && e == true &&
        res == 11111 && o == 3 && q == 4) {
        return 1
    }
    return -1
}

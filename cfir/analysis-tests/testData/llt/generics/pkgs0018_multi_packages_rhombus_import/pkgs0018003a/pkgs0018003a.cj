// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs0018003a

internal import pkgs0018001a.*

public func NormalFunc(a: Float32): Float32 {
    return a
}

public func Foo<T, U>(a: T, b: U): T {
    return a
}

public func Bar<T>(a: T) {
    var b: Float32 = 1.0
    var c = Foo<T, Float32>(a, b)
    return c
}

public struct Test<T> {
    public let member1: Float32
    public var member2: T
    public init(a: Float32, b: T) {
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
    public init(x: Float32, b: B) {
        a = b
    }
    public init(b: B) {
        a = b
    }
}

public func run2(): Int32 {
    var obj1: C1<Float32> = C1<Float32>(0.0)
    obj1.a = 1.0
    var ans1 = obj1.a + obj1.test() * 10.0 + obj1.test1(100.0) // 111
    var obj2 = C2<Float32>(0, 1000.0)
    var obj3 = C2<Float32>(10000.0)
    var obj4 = C2<Float64>(100000.0)
    var ans2 = obj2.a + obj3.a + Float32(obj4.a) + ans1 // 111000 + 111 = 111111

    var value = Test<Float32>(1.0, 10.0)
    var b = value.member1 + value.member2
    var c = value.testNormal(false)
    var d = value.testGeneric1(123.0)
    var e = value.testGeneric2(123.0, true)

    var f: R1<Float32> = R1<Float32>(0.0)
    let f_fake_test = f.a + 1.0
    var g = f_fake_test + f.test1(10.0) // 11
    var h = R1<Float32>(100.0)
    var i = R1<Float64>(1000.0)
    var j = h.a + Float32(i.test()) // 1100
    var k = R2<Float32>(0.0, 0.0)
    var l = R2<Float32>(10000.0)
    var m = k.a + l.a // 10000
    var res = g + j + m

    var n: Float32 = 3.0
    var o = NormalFunc(n)
    var p: Float32 = 4.0
    var q = Bar<Float32>(p)

    if (ans2 == 111111.0 && value.member1 == 1.0 && value.member2 == 10.0 && b == 11.0 && c == false && d == 123.0 &&
        e == true && res == 11111.0 && o == 3.0 && q == 4.0) {
        return 2
    }
    return -2
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class Fox<T> {
    public var fox: T
    public init(f: T) {
        fox = f
    }
}

open class Bar<T, U> <: Fox<Fox<T>> {
    public var bar: T
    public init(b: T) {
        super(Fox<T>(b))
        bar = b
    }
    public func bar1(): Fox<T> {
        print("bar1 is called")
        return this.fox
    }
}

open class Foo<T> <: Bar<T, Int32> {
    public var foo: T
    public init(f: T) {
        super(f)
        foo = f
    }
    public func foo1(): T {
        print("foo1 is called")
        return this.bar
    }
}

class Dog<T> <: Foo<T> {
    public var dog: T
    public init(a: T) {
        super(a)
        dog = a
    }
}

main(): Int64 {
    var d = Dog<Int32>(0)
    var f: Foo<Int32> = Foo<Int32>(0);
    var t: Fox<Int32> = f.bar1()
    return 0;
}

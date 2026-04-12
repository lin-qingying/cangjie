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

open class Bar<T, U> <: Fox<T> {
    public var bar: T
    public init(b: T) {
        super(b)
        bar = b
    }
    public func bar1(): T {
        return this.fox
    }
}

class Foo<T> <: Bar<T, Int32> {
    public var foo: T
    public init(f: T) {
        super(f)
        foo = f
    }
    public func foo1(): T {
        return this.bar
    }
}

main(): Int64 {
    var c: Foo<Int32> = Foo<Int32>(0)
    c.bar = 1
    print(c.fox.toString())
    c.bar1();
    return 0;
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output

// Was failed for rearranging. Should compile success, but do not run
interface fatherInterface<T> {
    func foo1(): Int8
    func foo3(): Int8
    func foo2(): Int8 {
        foo1()
    }
}

interface MyInterface<T> <: fatherInterface<T> {
    func foo2(): Int8 {
        foo1() + foo2()
    } // infinite self recursive during run
}

class myClass<T> <: MyInterface<T> {
    var x: Int8 = 10
    func f() {
        x = 20
        return x
    }
    public func foo1(): Int8 {
        f()
    }
    public func foo3(): Int8 {
        x
    }
}

main() {
    var c1: fatherInterface<Int32> = myClass<Int32>()
    var c2: MyInterface<Int32> = myClass<Int32>()
    var c3: myClass<Int32> = myClass<Int32>()

    if (c1.foo2() == 40 && c1.foo3() == 20 && c2.foo2() == 40 && c2.foo3() == 20 && c3.foo2() == 40 && c3.x == 20) {
        return 0
    } else {
        return 1
    }
}

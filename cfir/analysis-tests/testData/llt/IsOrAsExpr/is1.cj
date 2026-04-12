// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class Base {
    public var name: String = "Alice"
}

class Derived1 <: Base {
    public var age: UInt8 = 18
}

class Derived2 <: Base {
    public var gender: String = "female"
}

main() {
    var testVT = 1 is Int64 // testVT = true
    if (!testVT) {
        return 1
    }
    testVT = 1 is String // testVT = false
    if (testVT) {
        return 2
    }
    testVT = true is Int64 // testVT = false
    if (testVT) {
        return 3
    }
    testVT = [1, 2, 3] is Array<Int64> // testVT = true
    if (!testVT) {
        return 4
    }

    let base1: Base = Base()
    let base2: Base = Derived1()
    let base3: Base = Derived2()

    let derived1: Derived1 = Derived1()
    let derived2: Derived2 = Derived2()

    var test = base1 is Base // test = true
    if (!test) {
        return 5
    }
    test = base1 is Derived1 // test = false
    if (test) {
        return 6
    }
    test = base1 is Derived2 // test = false
    if (test) {
        return 7
    }
    test = base2 is Base // test = true
    if (!test) {
        return 8
    }
    test = base2 is Derived1 // test = true
    if (!test) {
        return 9
    }
    test = base2 is Derived2 // test = false
    if (test) {
        return 10
    }
    test = base3 is Base // test = true
    if (!test) {
        return 11
    }
    test = base3 is Derived1 // test = false
    if (test) {
        return 12
    }
    test = base3 is Derived2 // test = true
    if (!test) {
        return 13
    }

    test = derived1 is Base // test = true
    if (!test) {
        return 14
    }
    test = derived1 is Derived1 // test = true
    if (!test) {
        return 15
    }
    test = derived1 is Derived2 // test = false
    if (test) {
        return 16
    }
    test = derived2 is Base // test = true
    if (!test) {
        return 17
    }
    test = derived2 is Derived1 // test = false
    if (test) {
        return 18
    }
    test = derived2 is Derived2 // test = true
    if (!test) {
        return 19
    }
    return 0
}

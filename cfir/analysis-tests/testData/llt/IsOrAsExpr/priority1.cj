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

func CheckResult<T>(a: Option<T>): Bool {
    return match (a) {
        case Some(V) => true
        case None => false
    }
}

main(): Int64 {
    let base1: Base = Base()
    let derived1: Derived1 = Derived1()
    let derived2: Derived2 = Derived2()

    let res1 = true == base1 is Base
    if (!res1) {
        return 1;
    }
    let res2 = false == base1 is Derived1
    if (!res2) {
        return 2;
    }
    let res3 = false != base1 is Derived2
    if (res3) {
        return 3;
    }
    return 0
}

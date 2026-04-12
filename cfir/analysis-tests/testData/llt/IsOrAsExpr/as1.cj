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
    let base2: Base = Derived1()
    let base3: Base = Derived2()

    let derived1: Derived1 = Derived1()
    let derived2: Derived2 = Derived2()

    var castOP1 = base1 as Base // castOP1 = Option<Base>.Some(Base())
    if (!CheckResult<Base>(castOP1)) {
        return 1
    }
    var castOP2 = base1 as Derived1 // castOP2 = Option<Derived1>.None
    if (CheckResult<Derived1>(castOP2)) {
        return 2
    }
    var castOP3 = base1 as Derived2 // castOP3 = Option<Derived2>.None
    if (CheckResult<Derived2>(castOP3)) {
        return 3
    }
    var castOP4 = base2 as Base // castOP4 = Option<Base>.Some(Derived1())
    if (!CheckResult<Base>(castOP4)) {
        return 4
    }
    var castOP5 = base2 as Derived1 // castOP5 = Option<Derived1>.Some(Derived1())
    if (!CheckResult<Derived1>(castOP5)) {
        return 5
    }
    var castOP6 = base2 as Derived2 // castOP6 = Option<Derived2>.None
    if (CheckResult<Derived2>(castOP6)) {
        return 6
    }
    var castOP7 = base3 as Base // castOP7 = Option<Base>.Some(Derived2())
    if (!CheckResult<Base>(castOP7)) {
        return 7
    }
    var castOP8 = base3 as Derived1 // castOP8 = Option<Derived1>.None
    if (CheckResult<Derived1>(castOP8)) {
        return 8
    }
    var castOP9 = base3 as Derived2 // castOP9 = Option<Derived2>.Some(Derived2())
    if (!CheckResult<Derived2>(castOP9)) {
        return 9
    }
    var castOP10 = derived1 as Base // castOP10 = Option<Base>.Some(Derived1())
    if (!CheckResult<Base>(castOP10)) {
        return 10
    }
    var castOP11 = derived1 as Derived1 // castOP11 = Option<Derived1>.Some(Derived1())
    if (!CheckResult<Derived1>(castOP11)) {
        return 11
    }
    var castOP12 = derived1 as Derived2 // castOP12 = Option<Derived2>.None
    if (CheckResult<Derived2>(castOP12)) {
        return 12
    }
    var castOP13 = derived2 as Base // castOP13 = Option<Base>.Some(Derived2())
    if (!CheckResult<Base>(castOP13)) {
        return 13
    }
    var castOP14 = derived2 as Derived1 // castOP14 = Option<Derived1>.None
    if (CheckResult<Derived1>(castOP14)) {
        return 14
    }
    var castOP15 = derived2 as Derived2 // castOP15 = Option<Derived2>.Some(Derived2())
    if (!CheckResult<Derived2>(castOP15)) {
        return 15
    }
    return 0
}

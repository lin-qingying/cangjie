// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum ET {
    A
    | B
    | C

    public func printValue(): Unit {
        let res = match (this) {
            case A => "A"
            case B => "B"
            case C => "C"
        }
        print("the value is ${res}\n")
    }

    public func foo1(): Int64 {
        print("function foo1 defined in enum\n")
        return 1
    }

    public func foo1(a: Int64): Int64 {
        print("function foo1(Int64) defined in enum\n")
        return 1
    }
}

main() {
    let e1: ET = ET.A
    e1.printValue()
    e1.foo1()
    let e2: ET = ET.B
    e2.printValue()
    e2.foo1(1)
    let e3: ET = ET.C
    e3.printValue()
    e3.foo1(2)

    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum ET {
    A
    | B(String)
    | C(String, String)

    public func printValue(): Unit {
        let res = match (this) {
            case A => "A"
            case B(x) => "B(${x})"
            case C(x, y) => "C(${x}, ${y})"
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

    public func foo2(): Int64 {
        print("function foo2 defined in enum\n")
        return 1
    }
    private func foo3(): Int64 {
        print("function foo3 defined in enum\n")
        return 1
    }

    public static func foo4(): Int64 {
        print("function foo4 defined in enum\n")
        return 1
    }

    public operator func -(): ET {
        let res = match (this) {
            case A => ET.A
            case B(x) => ET.B("minus ${x}")
            case C(x, y) => ET.C("minus ${x}", "minus ${y}")
        }
        return res
    }

    public operator func +(rhs: ET): ET {
        let res = match (this) {
            case B(x) => match (rhs) {
                case B(y) => ET.B("${x} ${y}")
                case _ => ET.A
            }
            case C(x, y) => match (rhs) {
                case C(xx, yy) => ET.C("${x} ${xx}", "${y} ${yy}")
                case _ => ET.A
            }
            case A => ET.A
        }
        return res
    }
}

main() {
    let e1: ET = ET.A
    e1.printValue()
    let e2: ET = ET.B("one")
    e2.printValue()
    let e3: ET = ET.C("one", "two")
    e3.printValue()
    let e4 = -e1
    e4.printValue()
    let e5 = -e2
    e5.printValue()
    let e6 = -e3
    e6.printValue()
    let e7 = e1 + e2
    e7.printValue()
    let e8 = e2 + e3
    e8.printValue()
    let e9 = e1 + e3
    e9.printValue()
    let e10 = e2 + e2
    e10.printValue()
    let e11 = e3 + e3
    e11.printValue()
    e1.foo1()
    e1.foo1(100)
    e2.foo2()
    ET.foo4()

    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %enableO2 %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix 2>&1 | compare %f
// ASSERT: scan BBB999

class C <: ToString {
    public var x: Int32 = 999
    public func toString() {
        return x.toString()
    }
}

enum E<T> where T <: ToString {
    A | B(T) | ...
}

func f<T>(e: E<T>) where T <: ToString {
    match (e) {
        case A => println("AAA")
        case B(c) =>
            print("BBB");
            println(c)
        case _ => println("unknown")
    }
}

main() {
    f(E<C>.B(C()))
    return 0
}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args | compare %f

interface I {
    static func f1(): Unit {
        println("I")
    }
    static func f2(): Unit {
        f1()
    }
    func f3(): Unit {
        f1()
    }
    func f4(): Unit
    func f5(): Unit {
        f4()
    }
}

open class C <: I {
    public static func f1(): Unit {
        println("C f1")
    }
    public open func f4(): Unit {
        println("C f4")
    }
}

class D <: C {
    public static func f1(): Unit {
        println("D f1")
    }
    public override func f4(): Unit {
        println("D f4")
    }
}

main() {
    D.f2()
    D().f3()
    D().f5()
}

// (CJNATIVE)ASSERT: scan-2 D f1
// (not CJNATIVE) ASSERT: scan-2 C f1
// ASSERT: scan D f4

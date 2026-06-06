// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output | compare %f

interface I<T> {
    func f1(<!chir_dce_unused_variable!>a<!>: T): Unit {
        println("I")
    }
    func f2(a: T): Unit {
        f1(a)
    }
}

open class A {}

extend A {
    public func f1(<!chir_dce_unused_variable!>a<!>: Int64): Unit {
        println("a")
    }
    public func f1(a: String): Unit {
        println(a)
    }
}

class B <: A & I<Int64> & I<String> {}

main() {
    B().f2(1)
    B().f2("1")
}

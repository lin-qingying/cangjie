// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %f -o %n.%suffix %enableO2 --fno-chir-function-inlining
// (CJNATIVE) EXEC: %run %run_opt %n.%suffix 2>&1 | compare %f
// (CJNATIVE) ASSERT: scan 11
// (CJNATIVE) ASSERT: scan 12

abstract class A<U> {
    public static func f() {
        return 11
    }

    func check_instance_func_A() {
        println(f())
    }
}

class C2<U> <: A<U> {
    public static redef func f() {
        return 12
    }
}

main() {
    let C2_typeA: A<Unit> = C2<Unit>()
    println(A<Unit>.f())
    C2_typeA.check_instance_func_A()
}

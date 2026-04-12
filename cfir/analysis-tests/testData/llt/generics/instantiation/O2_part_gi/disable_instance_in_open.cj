// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f %enableO2 -o %output
// RUN-EXEC: %run %run_opt %output %run_args 2>&1 | compare %f

interface I {
    static func f6<T>(): Int64
    // Every member is accessible in instance functions
    func check_instance() {
        print(f6<Unit>())
    }
}

class A <: I {
    public static func f6<T>() {
        return 6
    }
}

main() {
    let tst = A()
    tst.check_instance()
}

// ASSERT: scan 6

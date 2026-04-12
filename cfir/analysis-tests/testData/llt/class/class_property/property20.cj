// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %cmp_opt %f -o %output
// EXEC-0: %run %run_opt %output %run_args

open class A {
    public mut static prop v: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

class B <: A {
    public mut static prop v: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

main() {
    return B.v - 1
}

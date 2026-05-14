// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class C1 {}

extend C1 {
    public prop foo: Int64 {
        get() {
            return 1
        }
    }
}

class C2 <: C1 {
    public prop <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>: Int64 {
        get() {
            return 2
        }
    }
}

main() {
    return -1
}

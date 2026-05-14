// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct Foo {
    public var ccc: Int32 = 2
    public prop ddd: Int32 {
        get() {
            <!CANNOT_MODIFY_VAR!>ccc<!> = 1
            return ccc
        }
    }
}

main(): Int32 {
    var f = Foo()
    let t = f.ddd
    return t
}

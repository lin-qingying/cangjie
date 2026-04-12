// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class A {}

interface Foo {
    prop v1: Int64
}

extend A <: Foo {
    public prop v1: Int64 {
        get() {
            3
        }
    }
}

class A1 {
    public prop v1: Int64 {
        get() {
            5
        }
    }
}

interface Foo1 {
    prop v1: Int64
}

extend A1 <: Foo1 {}

main() {
    0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

class C1<T> {}

class C2<T> {}

interface A {
    func foo(a: C1<Int32>): Int32 {
        return 0
    }
}

interface B {
    func foo(a: C2<Int32>): Int32 {
        return 1
    }
}

class D <: A & B {}

main() {
    var obj = D()
    if (obj.foo(C2<Int32>()) == 1) {
        return 0
    }
    return 1
}

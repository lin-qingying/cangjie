// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

class C1<T, X> {}

interface A {
    func foo(a: C1<Int32, Int64>): Int32 {
        return 0
    }
}

interface B {
    func foo(a: C1<Int32, Int64>): Int32 {
        print("calling foo() in B\n")
        return 1
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> <: A & B {}

main() {
    return 1
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %diag_json %f 2>&1 | compare %f

class E1 {}

class E2 {}

interface I {
    static func foo(): E1 {
        return E1()
    }
}

class D<T> where T <: C1 & I {
    public func foo() {
        return T.foo()
    }
}

class C1 <: I {
    public static func foo(): E2 {
        return E2()
    }
}

main() {
    let obj = D<C1>().foo()
    return 1
}

// ASSERT: scan-1 sema_inherit_member_type_inconsistent
// ASSERT: scan-1 sema_return_type_incompatible
// ASSERT: scan "Errors": 2

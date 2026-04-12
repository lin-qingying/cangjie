// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %diag_json %f 2>&1 | compare %f

open class C {}

extend C {
    public func foo(): Int {
        1
    }
}

interface I {
    func foo(): String
}

func bar<T>(x: T) where T <: C & I {
    return x.foo()
}

main() {
    return 1
}

// ASSERT: scan-1 sema_inherit_member_type_inconsistent
// ASSERT: scan "Errors": 1

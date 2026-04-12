// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %diag_json %f 2>&1 | compare %f

// Error reported on S <: I2. Constraint of 'func bar' should not report error.
struct S <: I2 {
    func foo(): Int {
        1
    }
}

interface I2 {
    func foo(): String
}

func bar<T>(x: T) where T <: S & I2 {
}

class A<T> where T <: S & I2 {}

main() {
}

// ASSERT: scan-1 sema_return_type_incompatible
// ASSERT: scan-2 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 3

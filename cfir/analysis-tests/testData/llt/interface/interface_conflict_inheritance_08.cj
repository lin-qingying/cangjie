// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %diag_json %f 2>&1 | compare %f

interface I1 {
    func foo(): Int
}

interface I2 {
    func foo(): String
}

class A<T> where T <: I2 {
    func test<X>() where X <: I1 & T {}
}

func f<T1, T2, T3>() where T1 <: T2 & I1, T2 <: T3, T3 <: I2 {}

main() {
}

// ASSERT: scan-2 sema_inherit_member_type_inconsistent
// ASSERT: scan-3 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 5

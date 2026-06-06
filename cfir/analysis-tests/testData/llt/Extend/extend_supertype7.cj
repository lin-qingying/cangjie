<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {
    func foo(): Unit
}

open class A {
    protected func foo(): Unit {}
}

class B <: A {}

extend <!sema_weak_visibility!>B<!> <: I {}

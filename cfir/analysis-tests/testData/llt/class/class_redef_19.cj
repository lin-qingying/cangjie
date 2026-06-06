// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %diag_json %f 2>&1 | compare %f

class C<T> {}

open class A {
    public static func foo<<!sema_upper_bound_must_be_class_or_interface!>T<!>>(a: C<T>) where T <: Int32 {
        return 1
    }
}

public open class B <: A {
    public static redef func <!sema_accessibility!>foo<!><<!sema_upper_bound_must_be_class_or_interface!>T<!>>(a: C<T>) where <!sema_generic_constraint_not_looser!>T <: Int64<!> {
        return 1
    }
}

main() {
    return 0
}

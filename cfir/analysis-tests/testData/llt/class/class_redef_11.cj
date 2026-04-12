// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %diag_json %typecheck %f 2>&1 | compare %f

main(): Int64 {
    if (Coo.foo<Int32>(1, 2) != 2 || Boo<Int32>.foo<Int32>(3, 4) != 4) {
        return 1
    }
    return 0
}

open class Boo<T> {
    public static func foo<V>(x: T, y: V) where V <: Int32 {
        return y
    }
}

class Coo <: Boo<Int32> {
    public static redef func foo<V>(x: Int32, y: V) where V <: Int64 {
        return y
    }
}

// ASSERT: scan-1 sema_generic_constraint_not_looser
// ASSERT: scan-1 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-2 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 4

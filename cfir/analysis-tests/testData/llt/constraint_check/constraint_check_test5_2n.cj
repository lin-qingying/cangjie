// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %diag_json %f 2>&1 | compare %f
// UpperBound: Tuple Type

func foo<T>() where T <: (Int32, Int32) {
    return 0
}

open class Base {}

class Child <: Base {}

class C {}

func bar<T>() where T <: (Int32, Child) {
    return 0
}

main(): Int64 {
    foo<(Int32, Int64)>()
    bar<(Int32, Base)>()
    bar<(Int32, Child)>() // OK
    bar<(Int32, C)>()
    bar<(Int64, Child)>()
    bar<(C, Child)>()
}

// ASSERT: scan-5 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-2 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 7

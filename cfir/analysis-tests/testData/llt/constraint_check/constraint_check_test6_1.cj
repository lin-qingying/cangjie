// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %diag_json %f 2>&1 | compare %f

// UpperBound: Array Type

func foo<T>() where T <: Array<Int32> {
    return 0
}

open class Base {}

class Child <: Base {}

func bar<T>() where T <: Array<(Int32, Base)> {
    return 0
}

func jay<T>() where T <: (Int32, Array<Base>) {
    return 0
}

main(): Int64 {
    foo<Array<Int32>>()
    bar<Array<(Int32, Base)>>()
    bar<Array<(Int32, Child)>>()
    jay<(Int32, Array<Base>)>()
    jay<(Int32, Array<Child>)>()
}

// ASSERT: scan-2 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-3 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 5

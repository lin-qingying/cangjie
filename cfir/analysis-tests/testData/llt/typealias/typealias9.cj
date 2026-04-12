// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %diag_json %f -o %n.%suffix 2>&1 | compare %f

type A<T> = Cl<T>

open class Base {}

class Cl<U> where U <: Base {}

class Sub<X> {
    public var a: A<X>
}

main() {
    return 0;
}

// ASSERT: scan-1 sema_class_uninitialized_field
// ASSERT: scan-1 sema_generic_type_argument_not_match_constraint
// ASSERT: scan "Errors": 2

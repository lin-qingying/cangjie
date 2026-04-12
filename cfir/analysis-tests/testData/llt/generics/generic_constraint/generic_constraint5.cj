// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %diag_json %f 2>&1 | compare %f

class C1 {}

class C2 {}

class A<X> {
    public func foo<Y>(x: X) where Y <: X {
        x
    }
    public func foo<Y>() {}
    public static func goo<Y>(x: X) where Y <: X {
        x
    }
}

open class Base {}

open class Sub <: Base {}

class SSub <: Sub {}

class Sub2 <: Base {}

class B<X> where X <: Sub {
    public func f<Y>(x: Y): X where Y <: X {
        x
    }
}

main(): Int64 {
    let a = A<C1>()
    a.foo<C2>(C1())
    let b = a.foo<C2>
    let c = A<C1>.goo<C2>

    B<Sub>().f<Sub2>(Sub2())
    let x: Sub = B<Sub>().f<SSub>(SSub())
    0
}

// ASSERT: scan-1 sema_ambiguous_use
// ASSERT: scan-3 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-3 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 7

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class C {
    public let a = 0
}

func Foo<U>(a: U) where U <: C {
    var b = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>U<!>.a
    return b
}

class C1 <: C {}

main() {
    return Foo<C1>(C1())
}

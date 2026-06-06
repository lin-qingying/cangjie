// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %diag_json %f 2>&1 | compare %f

class Z<T> {
    public func foo() {}
}

<!sema_generic_type_argument_not_match_constraint!>c<!>lass C<X> where X <: Z<C<<!sema_generic_type_argument_not_match_constraint!>I<!>nt32>> {
    init(input: X) {
        let a: Unit = input.foo()
    }
}

main(): Int64 {
    return 0
}

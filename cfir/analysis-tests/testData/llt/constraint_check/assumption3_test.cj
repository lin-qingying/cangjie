// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


<!sema_generic_type_argument_not_match_constraint!>c<!>lass C<X> where X <: C<C<C<C<<!sema_generic_type_argument_not_match_constraint!>I<!>nt32>>>> {
    func foo() {}
    public init(input: X) {
        let a: Unit = input.foo()
    }
}

<!sema_generic_type_argument_not_match_constraint!>i<!>nterface I<X> where X <: I<I<I<I<<!sema_generic_type_argument_not_match_constraint!>I<!>nt32>>>> {}

main(): Int64 {
    return 0
}

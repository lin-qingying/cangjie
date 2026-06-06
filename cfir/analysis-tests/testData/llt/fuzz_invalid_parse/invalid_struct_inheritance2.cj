// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was stackoverflow during type substitution. should pass
interface A<T> <: <!parse_decl_cannot_inherit_their_self!>A<C<T>><!> &B<C<T>>{}

<!parse_expected_decl!>clars<!> D <: C<<T> where T<:B<T>{}
interface B<T> where T<:A<T>{}

open class C<T> <: A<C<T>> &B<C<T>>{}

class D <: C<D>{}

main() {
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

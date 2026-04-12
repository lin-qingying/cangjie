// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was stackoverflow during type substitution. should pass
interface A<T> <: A<C<T>> &B<C<T>>{}

clars D <: C<<T> where T<:B<T>{}
interface B<T> where T<:A<T>{}

open class C<T> <: A<C<T>> &B<C<T>>{}

class D <: C<D>{}

main() {
    return 0
}

/* SCAN
error: declaration 'A' cannot inherit itself
 ==> invalid_struct_inheritance2.cj:4:19:
  | 
4 | interface A<T> <: A<C<T>> &B<C<T>>{}
  |           ~       ^^^^^^^ illegal super declaration here
  |           |
  |           because 'A' cannot inherit itself
  | 

error: expected declaration, found 'clars'
 ==> invalid_struct_inheritance2.cj:6:1:
  | 
6 | clars D <: C<<T> where T<:B<T>{}
  | ^^^^^ expected declaration here
  | 
  # note: only declarations or macro expressions can be used in the top-level

2 errors generated, 2 errors printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface A<T> <: B<C<T>> {}

interface B<T> {}

open class C<T> <: A<C<T>> {}

main(): Int64 {
    return 0
}

/* SCAN
error: generic infinite instantiation
  ==> generic_infinite_init_05.cj:13:20:
   | 
13 | open class C<T> <: A<C<T>> {}
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/

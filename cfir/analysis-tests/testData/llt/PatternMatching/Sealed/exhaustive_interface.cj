// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

sealed interface I<T> {}

private class A <: I<Int64> {}

private class B <: I<Rune> {}

private class C<T> <: I<T> where T <: ToString {}

func f(x: I<Int64>) {
    match (x) {
        case _: A => 0
        case _: B => -1 // unreachable
        case _: C<Int64> => 1
        case _: C<Rune> => -1 // unreachable
        case _ => -1
    }
}

main() {
    f(A())
}

/* SCAN
warning: unreachable pattern
  ==> exhaustive_interface.cj:21:14:
   | 
21 |         case _: B => -1 // unreachable
   |              ^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> exhaustive_interface.cj:23:14:
   | 
23 |         case _: C<Rune> => -1 // unreachable
   |              ^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/

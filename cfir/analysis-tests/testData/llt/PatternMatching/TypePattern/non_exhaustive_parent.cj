// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

private interface I {}

private class A <: I {}

private interface J <: I {}

private class B {}

extend B <: J {}

func f(x: Option<I>) {
    match (x) {
        case Some(_: A) => 0
        case Some(_: J) => 1
        case Some(_: B) => -1 // unreachable
    }
}

main() {
    f(Some(0))
}

/* SCAN
warning: unreachable pattern
  ==> non_exhaustive_parent.cj:23:14:
   | 
23 |         case Some(_: B) => -1 // unreachable
   |              ^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: non-exhaustive patterns
  ==> non_exhaustive_parent.cj:20:12:
   | 
20 |     match (x) {
   |            ^ the selector is of type 'Enum-Option<Interface-I>'
   | 
   # note: Some(_) is not covered
   # note: None is not covered

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/

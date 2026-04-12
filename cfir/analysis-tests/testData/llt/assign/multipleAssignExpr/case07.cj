// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output
// (CJNATIVE) RUN-EXEC: %run %run_opt %output %run_args
// (not CJNATIVE) ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {}

class A {}

extend A <: I {}

func get(): ((A, A), Int64) {
    return ((A(), A()), 1)
}

main() {
    var i: (I, I)
    var val = 1
    (i, val) = get()
    0
}

/* SCAN
error: mismatched types
  ==> case07.cj:15:16:
   |
15 |     (i, val) = get()
   |      ~         ^^^^^ the expression has type 'Tuple<Tuple<Class-A, Class-A>, Int64>'
   |      |
   |      can not assign 'Tuple<Interface-I, Interface-I>' with 'Tuple<Class-A, Class-A>'
   |

1 error generated, 1 error printed.
 */

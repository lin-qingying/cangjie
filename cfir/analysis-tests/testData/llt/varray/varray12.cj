// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args

func foo() {
    var i = 0
    while (i < 5) {
        i++;
        let arr1: VArray<Nothing, $3> = [continue, break, return 0]
        arr1[0]
    }
    if (i != 5) {
        return 1
    }
    return 0
}

func foo1() {
    var i = 0
    while (i < 5) {
        i++;
        let arr1: VArray<Int64, $3> = [1, break, return 0]
        arr1[0]
    }
    if (i != 1) {
        return 1
    }
    return 0
}

main() {
    return foo() + foo1()
}

/* SCAN
warning: unreachable 'expression'
 ==> varray12.cj:8:48:
  |
8 |     let arr1: VArray<Nothing, $3> = [continue, break, return 0]
  |                                      ~~~~~~~~  ^^^^^ unreachable 'expression'
  |                                      |
  |                                      any code following this expression is unreachable
  |
  # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused expression
  ==> varray12.cj:21:36:
   |
21 |     let arr1: VArray<Int64, $3> = [1, break, return 0]
   |                                    ^ unused expression
   |
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable 'expression'
  ==> varray12.cj:21:46:
   |
21 |     let arr1: VArray<Int64, $3> = [1, break, return 0]
   |                                       ~~~~~  ^^^^^^^^ unreachable 'expression'
   |                                       |
   |                                       any code following this expression is unreachable
   |
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

3 warnings generated, 3 warnings printed.
 */

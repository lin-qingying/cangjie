// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

main() {
    let v: Int64 = if (match {
        case _ =>
            Option<Unit>.None()
            true
    }) {
        1
    } else {
        2
    } + 1
    return 0
}

/* SCAN
error: no matching function for operator '()' function call
  ==> binary_error_report_03.cj:12:13:
   | 
12 |             Option<Unit>.None()
   |             ^ 
   | 

1 error generated, 1 error printed.
*/

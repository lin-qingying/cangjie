// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

public enum E {
    A(Int64)
}

func foo(name!: E) {
    true
}

main() {
    foo(E.A(1)) + 1
}

/* SCAN
error: missing argument prefix 'name:' for named parameter
  ==> binary_error_report_06.cj:18:9:
   | 
18 |     foo(E.A(1)) + 1
   |         ^^^^^^ 
   | 

1 error generated, 1 error printed.
*/

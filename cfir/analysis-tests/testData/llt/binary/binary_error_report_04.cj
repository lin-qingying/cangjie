// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

// Test for error reporting was not able to report error in main's binaryExpr
interface I<T> {}

func foo(): Unit {
    try {
        try {
            ();
        } finally {
            ((UInt64(160) & 127)..(UInt64(61) & 127) : ((-85) | 1))
        }
    } catch (_) {
    }
    ()
}

func test<T>(): UInt64 {
    1
}

main() {
    1 ** test<I>()
    return 0
}

/* SCAN
error: generic type should be used with type argument
  ==> binary_error_report_04.cj:29:15:
   | 
29 |     1 ** test<I>()
   |               ^ 
   | 

1 error generated, 1 error printed.
*/

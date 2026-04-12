// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %cmp_opt %f 2>&1 | compare %f

class A <: ToString {
    public func toString() {
        "a"
    }
}

main(): Int64 {
    var a = A()
    print(a)
}

/* SCAN
error: mismatched types
  ==> call_error_report_03.cj:17:5:
   | 
17 |     print(a)
   |     ^^^^^^^^ expected 'Int64', found 'Unit'
   | 

1 error generated, 1 error printed.
*/

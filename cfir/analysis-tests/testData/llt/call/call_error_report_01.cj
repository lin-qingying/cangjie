// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %cmp_opt %f 2>&1 | compare %f

internal import std.collection.ArrayList

func test() {
    var metrics: ArrayList<Int64> = ArrayList<Int64>([Int64(UndeclaredType())])
}

main(): Int64 {
    test()
    return 0
}

/* SCAN
error: undeclared identifier 'UndeclaredType'
  ==> call_error_report_01.cj:12:61:
   | 
12 |     var metrics: ArrayList<Int64> = ArrayList<Int64>([Int64(UndeclaredType())])
   |                                                             ^ 
   | 

1 error generated, 1 error printed.
*/

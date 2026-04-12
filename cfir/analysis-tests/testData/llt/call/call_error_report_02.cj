// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %cmp_opt %f 2>&1 | compare %f

internal import std.collection.ArrayList

main(): Int64 {
    var a1 = "abcdefg"
    var temp1 = "c"
    var temp2 = UInt8(UInt32(r'c'))
    var res1 = a1.indexOf(temp1, "12")
    var res2 = a1.indexOf(temp2, "12")
    return 0
}

/* SCAN
error: mismatched types
  ==> call_error_report_02.cj:15:34:
   | 
15 |     var res1 = a1.indexOf(temp1, "12")
   |                                  ^^^^ expected 'Int64', found 'Struct-String'
   | 

error: mismatched types
  ==> call_error_report_02.cj:16:34:
   | 
16 |     var res2 = a1.indexOf(temp2, "12")
   |                                  ^^^^ expected 'Int64', found 'Struct-String'
   | 

2 errors generated, 2 errors printed.
*/

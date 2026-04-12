// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

main() {
    var arr = [1, 2, 3, 4, 5]
    var range1 = 1..10 : 3
    var arr1 = arr[range1]
    var arr2 = arr[1..10 : 2.0]
    let v: Collection<Int32> = 1..100
    return 0
}

/* SCAN
error: cannot convert a floating-point literal to type 'Int64'
  ==> range4.cj:13:28:
   | 
13 |     var arr2 = arr[1..10 : 2.0]
   |                            ^ 
   | 

error: mismatched types
  ==> range4.cj:14:32:
   | 
14 |     let v: Collection<Int32> = 1..100
   |                                ^^^^^^ expected 'Interface-Collection<Int32>', found 'Struct-Range<Int64>'
   | 

2 errors generated, 2 errors printed.
*/

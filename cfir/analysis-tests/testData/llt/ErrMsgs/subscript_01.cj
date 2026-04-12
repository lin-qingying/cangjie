// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

// Should reporting type mismatch, not just invalid subscript expr
func test(a: Array<Int8>): Unit {}

main() {
    var arr: Array<Array<UInt64>> = [[255u64, 0u64, 0u64, 0u64]]
    var a = test(arr[0])
}

/* SCAN
error: mismatched types
  ==> subscript_01.cj:14:18:
   | 
14 |     var a = test(arr[0])
   |                  ^^^^^^ expected 'Struct-Array<Int8>', found 'Struct-Array<UInt64>'
   | 

1 error generated, 1 error printed.
*/

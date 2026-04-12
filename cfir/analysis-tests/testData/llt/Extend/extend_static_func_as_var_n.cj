// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend Int64 {
    static func test() {}
}

extend Int64 {
    static func test(a: Int64) {}
}

main() {
    var x = Int64.test
    return 0
}

/* SCAN
error: ambiguous use of 'test'
  ==> extend_static_func_as_var_n.cj:18:13:
   | 
18 |     var x = Int64.test
   |             ^^^^^^^^^^ 
   | 
note: found candidate
  ==> extend_static_func_as_var_n.cj:10:17:
   | 
10 |     static func test() {}
   |                 ^^^^ 
   | 
note: found candidate
  ==> extend_static_func_as_var_n.cj:14:17:
   | 
14 |     static func test(a: Int64) {}
   |                 ^^^^ 
   | 

1 error generated, 1 error printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

public class Test<T> {
    public var a: Int64 = 11
    private Test(x: T) {
        println("before")
    }
    public init(x: Int64) {
        Test<Bool>(true)
    }
}

main(): Int64 {
    Test<Int64>(1)
    return 0
}

/* SCAN
error: generic instantiation 'Test<Int64>' causes ambiguous function 'init'
  ==> inst_dup_functions007.cj:20:5:
   | 
20 |     Test<Int64>(1)
   |     ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions007.cj:11:13:
   | 
11 |     private Test(x: T) {
   |             ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions007.cj:14:12:
   | 
14 |     public init(x: Int64) {
   |            ^^^^ 
   | 

1 error generated, 1 error printed.
*/

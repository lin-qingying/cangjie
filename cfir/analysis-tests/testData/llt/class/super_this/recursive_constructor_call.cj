// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class Test {
    public init() {
        this(true)
    }

    public init(a: Bool) {
        this(true)
    }
}

main(): Int64 {
    var test: Test = Test()
    return 0
}

/* SCAN
error: recursive constructor calling detected
  ==> recursive_constructor_call.cj:14:12:
   | 
14 |     public init(a: Bool) {
   |            ^^^^ 
   | 
note: depends on
  ==> recursive_constructor_call.cj:15:9:
   | 
15 |         this(true)
   |         ^^^^ 
   | 
note: depends on
  ==> recursive_constructor_call.cj:14:12:
   | 
14 |     public init(a: Bool) {
   |            ^^^^ 
   | 

1 error generated, 1 error printed.
*/

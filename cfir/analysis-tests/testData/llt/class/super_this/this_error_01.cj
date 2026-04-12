// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class C1 {
    public func foo1() {
        this = C1()
    }
}

main() {
    return 1
}

/* SCAN
error: cannot assign a value to 'this'
  ==> this_error_01.cj:11:9:
   | 
11 |         this = C1()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/

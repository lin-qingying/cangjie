// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ok_class_08_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt -O2 ok_class_08_dep.cj -o pkga.%middle
// EXEC-PIPE-1: %compiler %cmp_opt pkga.%middle %f -o %output 2>&1 | compare %f

import pkga.*

@B
public class Test {
    @B
    public func testfunc() {
        println("testfunc")
    }
}

main() {}

/* SCAN-IN
error: '@B' not applicable to member function
  ==> ok_class_08.cj:15:5:
   | 
15 |     @B
   |     ^^ 
   | 
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

open class A {}

class B <: A {
    public operator func ==(right: B): Bool {
        return true;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (x == (B(), 1)) {
        return 0;
    }
    return 1;
}

/* SCAN
error: operator '==' between tuple type 'Tuple<Class-A, Int64>' and 'Tuple<Class-A, Int64>' is not supported
  ==> tupleEqual09.cj:19:9:
   | 
19 |     if (x == (B(), 1)) {
   |         ^ 
   | 

1 error generated, 1 error printed.
*/

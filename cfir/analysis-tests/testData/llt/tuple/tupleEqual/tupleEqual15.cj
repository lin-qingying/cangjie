// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

class B {}

main(): Int64 {
    let x = (A(), B());
    if (x == (A(), B())) {
        return 0;
    }
    return 1;
}

/* SCAN
error: operator '==' between tuple type 'Tuple<Class-A, Class-B>' and 'Tuple<Class-A, Class-B>' is not supported
  ==> tupleEqual15.cj:19:9:
   | 
19 |     if (x == (A(), B())) {
   |         ^ 
   | 

1 error generated, 1 error printed.
*/

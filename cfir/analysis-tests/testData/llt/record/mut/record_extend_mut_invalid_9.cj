// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo(): Unit {
        i += 1
    }

    var fn = {=> foo()} 
}

main() {
    return 1
}

/* SCAN
error: unexpected variable declaration in extend body
  ==> record_extend_mut_invalid_9.cj:18:5:
   | 
13 | extend R1 {
   | ~~~~~~ in extend
14 |     public mut func foo(): Unit {
   | ...
18 |     var fn = {=> foo()} 
   |     ^^^ unexpected variable declaration
   | 

1 error generated, 1 error printed.
*/

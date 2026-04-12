// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

struct Aoo {
    public var a: Int32 = 1
    public let b: Int32 = a
    public var c = if (true) {
        func foo2() {
            a
        }
    }
    public func foo3() {
        a
    }
}

/* SCAN
error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> capture_func_record_3601.cj:18:13:
   | 
18 |             a
   |             ^ 
   | 

1 error generated, 1 error printed.
*/

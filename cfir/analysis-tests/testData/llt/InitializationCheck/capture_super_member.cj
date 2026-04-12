// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

open class Boo {
    public var a: Int32 = 1
    public let b: Int32 = 2
}

class Aoo <: Boo {
    public var c = if (true) {
        func foo2() {
            var x = a;
            return x
        }
    }
}

/* SCAN
error: super member 'a' is not allowed to be used before calling super()
  ==> capture_super_member.cj:21:21:
   | 
21 |             var x = a;
   |                     ^ 
   | 

1 error generated, 1 error printed.
*/

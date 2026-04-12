// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class Boo {
    public var x: Int64
    public init(x: Int64) {
        this.x = x
    }
}

class Coo <: Boo {
    public var y: Int64
    public init(x: Int64) {
        try {
            super(x)
            this.y = x
        } catch (e: NegativeArraySizeException) {
            throw e
        }
    }
}

main() {}

/* SCAN
error: call to 'super' must be first expression in constructor of class 'Coo'
  ==> super_this_11.cj:20:13:
   | 
20 |             super(x)
   |             ^ 
   | 

1 error generated, 1 error printed.
*/

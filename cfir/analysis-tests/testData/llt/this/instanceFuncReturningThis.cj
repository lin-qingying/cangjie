// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt

public open class Base {
    public open func g(): This {
        println("return Base");
        Base().f()
    }
    public func f(): This {
        this
    }
}

public class Sub <: Base {
    private let x: Int64 = 10
    public func subOnly() {
        println("SubOnly: ${x}")
    }
}

main() {
    Sub().g().subOnly()
}

/* SCAN
error: mismatched types
  ==> instanceFuncReturningThis.cj:12:9:
   | 
12 |         Base().f()
   |         ^^^^^^^^^^ expected 'This', found 'Class-Base'
   | 

1 error generated, 1 error printed.
*/

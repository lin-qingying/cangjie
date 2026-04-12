// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public func toString() {
        "1"
    }
}

struct B {
    public func toString() {
        "1"
    }
}

class C <: ToString {
    public func toString() {
        "1"
    }
}

struct D <: ToString {
    public func toString() {
        "1"
    }
}

main() {
    print("${A()}")
    print("${B()}")
    print("${C()}")
    print("${D()}")
    return 0
}

/* SCAN
error: the type 'Class-A' should implement interface 'ToString' in the 'core' package
  ==> string1.cj:34:13:
   | 
34 |     print("${A()}")
   |             ^ 
   | 

error: the type 'Struct-B' should implement interface 'ToString' in the 'core' package
  ==> string1.cj:35:13:
   | 
35 |     print("${B()}")
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/

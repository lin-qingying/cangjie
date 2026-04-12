// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class A {
    public open func f(): String {
        return ""
    }
    public open prop a: Int {
        get() {
            0
        }
    }
}

abstract class B <: A {
    public func f(): String
    public prop a: Int
}

main() {}

/* SCAN
error: cannot override non-abstract function 'f' with abstract function
  ==> class_inheritance_8.cj:21:5:
   | 
21 |     public func f(): String
   |     ^ 
   | 

error: cannot override non-abstract property 'a' with abstract property
  ==> class_inheritance_8.cj:22:5:
   | 
22 |     public prop a: Int
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f %compile_lib_opt 2>&1 | compare %f

open class Father {}

class Child <: Father {}

interface J {
    func foo(a: Int16): Child {
        return Child()
    }
}

public class B <: J {
    public override func foo(a: Int16): Father {
        return Child()
    }
}

/* SCAN
error: return type of 'foo' is not identical or not a subtype of the overridden/redefined/implement function
  ==> class2.cj:20:26:
   | 
20 |     public override func foo(a: Int16): Father {
   |                          ^^^ 
   | 
note: 'Class-Father' is not a subtype of 'Class-Child'
  ==> class2.cj:14:10:
   | 
14 |     func foo(a: Int16): Child {
   |          ^^^ 
   | 

1 error generated, 1 error printed.
*/

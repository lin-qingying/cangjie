// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    public var a:Int64 =3

}

extend A {
   public static func g():Int64 {
        func f() {
            a
            0
        }
        0
    }
}

class B{
    public var a:Int64 =3

}

extend B {
    protected prop f: String { 
        get() {
            return "C"
        }
    }
       public open prop f1: String { 
        get() {
            return "C"
        }
    }
}

/* SCAN
error: unexpected modifier 'open' on property declaration in extend body
  ==> extend_property3.cj:35:15:
   | 
29 | extend B {
   | ~~~~~~ in extend
30 |     protected prop f: String { 
   | ...
35 |        public open prop f1: String { 
   |               ^^^^ ~~~~ on property declaration
   |               |
   |               unexpected modifier
   | 

1 error generated, 1 error printed.
*/

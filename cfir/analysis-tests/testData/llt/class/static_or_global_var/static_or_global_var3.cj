// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class A {
    public init() {}

    public var a: UInt
    public mut static prop v: UInt64 {
        get() {
            ()
            UInt64(123)
            b
        }
        set(Value) {
            Int64(40)
            ()
            b = Value
        }
    }
    static var b: UInt64
    static init() {}
    // Should not report error bellow
    func goo() {
        get()
    }
    func get() {
        a
        this.a
    }
}

main(): Int64 {
    1
}

/* SCAN
error: the static member variable 'b' is not initialized
  ==> static_or_global_var3.cj:25:5:
   | 
25 |     static var b: UInt64
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may add an initializer inline or initialize the variable in the 'static init'
note: static init defined here
  ==> static_or_global_var3.cj:26:12:
   | 
26 |     static init() {}
   |            ^^^^ 
   | 

error: the uninitialized member variable 'a' is not initialized in the constructor of class or struct
  ==> static_or_global_var3.cj:10:5:
   | 
10 |     public init() {}
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/

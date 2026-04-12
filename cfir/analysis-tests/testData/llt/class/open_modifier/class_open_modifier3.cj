// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    public open func f(): Int32 {
        return 0
    }
    public init() {
    }
}

class B <: A {
    public init() {
        f()
        var a = {=> super.f}
        func f3(c!: Int32 = f()) {
            f
        }
        a()
    }
    public open override func f(): Int32 {
        return 3
    }
}

class C <: B {
    public init() {
        super.f()
        var a = {=> f}
        func f3(c!: Int32 = f) {
            this.f
        }
        a()
    }
}

main(): Int64 {
    0
}

/* SCAN
warning: the current member should not have 'open' modifier because it is in a non-inheritable class
  ==> class_open_modifier3.cj:10:5:
   | 
10 |     public open func f(): Int32 {
   |     ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: the current member should not have 'open' modifier because it is in a non-inheritable class
  ==> class_open_modifier3.cj:26:5:
   | 
26 |     public open override func f(): Int32 {
   |     ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: super class 'A' is not inheritable
  ==> class_open_modifier3.cj:17:12:
   | 
17 | class B <: A {
   |            ^ 
   | 

error: super class 'B' is not inheritable
  ==> class_open_modifier3.cj:31:12:
   | 
31 | class C <: B {
   |            ^ 
   | 

error: mismatched types
  ==> class_open_modifier3.cj:35:29:
   | 
35 |         func f3(c!: Int32 = f) {
   |                             ^ expected 'Int32', found '() -> Int32'
   | 

3 errors generated, 3 errors printed.
2 warnings generated, 2 warnings printed.
*/

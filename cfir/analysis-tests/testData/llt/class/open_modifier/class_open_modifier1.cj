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
        f()
        var a = {=> f}
        func f3(c!: Int32 = f()) {
            f
        }
        a()
    }
}

main(): Int64 {
    0
}

/* SCAN
warning: the current member should not have 'open' modifier because it is in a non-inheritable class
  ==> class_open_modifier1.cj:10:5:
   | 
10 |     public open func f(): Int32 {
   |     ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'f3'
  ==> class_open_modifier1.cj:17:9:
   | 
17 |           func f3(c!: Int32 = f()) {
   |  _________^
18 | |             f
19 | |         }
   | |_________^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'c'
  ==> class_open_modifier1.cj:17:17:
   | 
17 |         func f3(c!: Int32 = f()) {
   |                 ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

3 warnings generated, 3 warnings printed.
*/

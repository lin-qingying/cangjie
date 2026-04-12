// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %frontendCompiler %typecheck %cmp_opt %f

enum E1 {
    EE | ER(Int32)

    public prop a: Int64 {
        get() {
            0
        }
    }
    public prop b: Int64 {
    }
    public prop c: Int64 {
        get() {}
        set(v) {}
    }
}

main(): Int64 {
    let a = EE
    let b = ER(3)
    a.a = 1
    b.b = 1
    b.d
    return 0
}

/* SCAN
error: property must have accessors
  ==> enum17_prop2.cj:17:5:
   | 
17 |       public prop b: Int64 {
   |  _____^
18 | |     }
   | |_____^
   | 

error: immutable property cannot have setter
  ==> enum17_prop2.cj:19:5:
   | 
19 |       public prop c: Int64 {
   |  _____^
20 | |         get() {}
   | | ...
22 | |     }
   | |_____^
   | 

error: mismatched types
  ==> enum17_prop2.cj:20:15:
   | 
20 |         get() {}
   |               ^^ expected 'Int64', found 'Unit'
   | 

error: 'd' is not a member of enum 'E1'
  ==> enum17_prop2.cj:30:7:
   | 
30 |     b.d
   |       ^ 
   | 

error: cannot assign to immutable value
  ==> enum17_prop2.cj:28:5:
   | 
28 |     a.a = 1
   |     ^^^^^^^ 
   | 
note: property 'a' is immutable
  ==> enum17_prop2.cj:12:17:
   | 
12 |     public prop a: Int64 {
   |                 ^ 
   | 

error: cannot assign to immutable value
  ==> enum17_prop2.cj:29:5:
   | 
29 |     b.b = 1
   |     ^^^^^^^ 
   | 
note: property 'b' is immutable
  ==> enum17_prop2.cj:17:17:
   | 
17 |     public prop b: Int64 {
   |                 ^ 
   | 

6 errors generated, 6 errors printed.
*/

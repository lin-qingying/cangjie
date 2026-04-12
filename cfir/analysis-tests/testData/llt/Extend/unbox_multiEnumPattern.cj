// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class B {
    func g(): Int64 {
        println("func g in B!")
        10
    }
}

interface I {
    func f(): Int64
}

extend B <: I {
    func f() {
        println("In extend B!");
        1
    }
}

main(): Int64 {
    var m: E = C(B())

    var res: Int64 = match (m) {
        case C(x: B) | D(x: B) => x.f();
        case _ => 0
    }

    var n: E = D(B())

    res = match (n) {
        case C(x: B) | D(x: B) => x.g()
        case _ => 0
    }
    res
}

enum E {
    C(I) | D(I)
}

/* SCAN
error: redefinition of declaration 'x'
  ==> unbox_multiEnumPattern.cj:31:26:
   | 
31 |         case C(x: B) | D(x: B) => x.f();
   |                          ^ 
   | 
note: 'x' is previously declared here
  ==> unbox_multiEnumPattern.cj:31:16:
   | 
31 |         case C(x: B) | D(x: B) => x.f();
   |                ^ 
   | 

error: redefinition of declaration 'x'
  ==> unbox_multiEnumPattern.cj:38:26:
   | 
38 |         case C(x: B) | D(x: B) => x.g()
   |                          ^ 
   | 
note: 'x' is previously declared here
  ==> unbox_multiEnumPattern.cj:38:16:
   | 
38 |         case C(x: B) | D(x: B) => x.g()
   |                ^ 
   | 

error: cannot introduce variables in patterns connected by '|'
  ==> unbox_multiEnumPattern.cj:31:16:
   | 
31 |         case C(x: B) | D(x: B) => x.f();
   |                ^         ~ 
   | 

error: cannot introduce variables in patterns connected by '|'
  ==> unbox_multiEnumPattern.cj:38:16:
   | 
38 |         case C(x: B) | D(x: B) => x.g()
   |                ^         ~ 
   | 

error: a deriving member must be at least as visible as its base member
  ==> unbox_multiEnumPattern.cj:21:10:
   | 
21 |     func f() {
   |          ^ the visibility of the deriving 'f' is 'internal'
   | 
note: the visibility of the base 'f' is 'public'
  ==> unbox_multiEnumPattern.cj:17:10:
   | 
17 |     func f(): Int64
   |          ^ the base 'f' is in 'interface'
   | 

5 errors generated, 5 errors printed.
*/

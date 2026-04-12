// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt


@Annotation[target: [GlobalVariable]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
func foo() {}

@Annotation[target: [MemberFunction]]
public class APMLevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APMLevel[11]
func bar() {}

class M {
    @APILevel[3]
    func bar() {}
    @APMLevel[3]
    var t = 3
}

main() {
    foo()
    bar()
    M().bar()
    0
}

/* SCAN
error: '@APILevel' not applicable to global function
  ==> globalvar_on_globalfunc.cj:18:1:
   | 
18 | @APILevel[11]
   | ^^^^^^^^^^^^^ 
   | 

error: '@APMLevel' not applicable to global function
  ==> globalvar_on_globalfunc.cj:29:1:
   | 
29 | @APMLevel[11]
   | ^^^^^^^^^^^^^ 
   | 

error: '@APILevel' not applicable to member function
  ==> globalvar_on_globalfunc.cj:33:5:
   | 
33 |     @APILevel[3]
   |     ^^^^^^^^^^^^ 
   | 

error: '@APMLevel' not applicable to member variable
  ==> globalvar_on_globalfunc.cj:35:5:
   | 
35 |     @APMLevel[3]
   |     ^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/

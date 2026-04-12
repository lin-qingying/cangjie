// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj json
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt canIUse.cj -o canIUse.%middle
// ERRCHECK: %compiler %cmp_opt %f canIUse.%middle canIUse.%middle --cfg=APILevel_syscap=json/config.json

// Note: 2in1:   [A, B] + [A, C] ===> [A, B, C]
//       phone:  [A, D] + [A, E] ===> [A, D, E]
//       tablet: [A, F] + [A, G] ===> [A, F, G]
// intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]

import ohos.labels.*
import ohos.base.*

@!APILevel[since: "19", syscap: "A"]
public func f_A() {}

@!APILevel[since: "19", syscap: "B"]
public func f_B() {}

@!APILevel[since: "19", syscap: "C"]
public func f_C() {}

@!APILevel[since: "19", syscap: "D"]
public func f_D() {}

@!APILevel[since: "19", syscap: "E"]
public func f_E() {}

@!APILevel[since: "19", syscap: "F"]
public func f_F() {}

@!APILevel[since: "19", syscap: "G"]
public func f_G() {}

@!APILevel[since: "19", syscap: "H"]
public func f_H() {}

@!APILevel[since: "19", syscap: "I"]
public func f_I() {}

main() {
    @IfAvailable(syscap: "H", {=> // intersectionSet: [A, H], unionSet: [A, B, C, D, E, F, G, H]
        f_A() // ok
        f_B() // warning
        f_C() // warning
        f_D() // warning
        f_E() // warning
        f_F() // warning
        f_G() // warning
        f_H() // ok
        f_I() // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        f_B() // warning
        f_C() // warning
        f_D() // warning
        f_E() // warning
        f_F() // warning
        f_G() // warning
        f_H() // error
        f_I() // error
    })

    @IfAvailable(syscap: "A", {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        f_B() // warning
        f_C() // warning
        f_D() // warning
        f_E() // warning
        f_F() // warning
        f_G() // warning
        f_H() // error
        f_I() // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        f_B() // warning
        f_C() // warning
        f_D() // warning
        f_E() // warning
        f_F() // warning
        f_G() // warning
        f_H() // error
        f_I() // error
    })

    @IfAvailable(syscap: "B", {=> // intersectionSet: [A, B], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        f_B() // ok
        f_C() // warning
        f_D() // warning
        f_E() // warning
        f_F() // warning
        f_G() // warning
        f_H() // error
        f_I() // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        f_B() // warning
        f_C() // warning
        f_D() // warning
        f_E() // warning
        f_F() // warning
        f_G() // warning
        f_H() // error
        f_I() // error
    })
}

/* SCAN
warning: inappropriate syscap 'B'
  ==> syscap_test01.cj:50:9:
   | 
50 |         f_B() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: H, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'C'
  ==> syscap_test01.cj:51:9:
   | 
51 |         f_C() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: H, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'D'
  ==> syscap_test01.cj:52:9:
   | 
52 |         f_D() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: H, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'E'
  ==> syscap_test01.cj:53:9:
   | 
53 |         f_E() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: H, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'F'
  ==> syscap_test01.cj:54:9:
   | 
54 |         f_F() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: H, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'G'
  ==> syscap_test01.cj:55:9:
   | 
55 |         f_G() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: H, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

error: inappropriate syscap 'I'
  ==> syscap_test01.cj:57:9:
   | 
57 |         f_I() // error
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: H, A, B...

warning: inappropriate syscap 'B'
  ==> syscap_test01.cj:60:9:
   | 
60 |         f_B() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'C'
  ==> syscap_test01.cj:61:9:
   | 
61 |         f_C() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'D'
  ==> syscap_test01.cj:62:9:
   | 
62 |         f_D() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'E'
  ==> syscap_test01.cj:63:9:
   | 
63 |         f_E() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'F'
  ==> syscap_test01.cj:64:9:
   | 
64 |         f_F() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'G'
  ==> syscap_test01.cj:65:9:
   | 
65 |         f_G() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

error: inappropriate syscap 'H'
  ==> syscap_test01.cj:66:9:
   | 
66 |         f_H() // error
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A, B, C...

error: inappropriate syscap 'I'
  ==> syscap_test01.cj:67:9:
   | 
67 |         f_I() // error
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A, B, C...

warning: inappropriate syscap 'B'
  ==> syscap_test01.cj:72:9:
   | 
72 |         f_B() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'C'
  ==> syscap_test01.cj:73:9:
   | 
73 |         f_C() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'D'
  ==> syscap_test01.cj:74:9:
   | 
74 |         f_D() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'E'
  ==> syscap_test01.cj:75:9:
   | 
75 |         f_E() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'F'
  ==> syscap_test01.cj:76:9:
   | 
76 |         f_F() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'G'
  ==> syscap_test01.cj:77:9:
   | 
77 |         f_G() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

error: inappropriate syscap 'H'
  ==> syscap_test01.cj:78:9:
   | 
78 |         f_H() // error
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A, B, C...

error: inappropriate syscap 'I'
  ==> syscap_test01.cj:79:9:
   | 
79 |         f_I() // error
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A, B, C...

warning: inappropriate syscap 'B'
  ==> syscap_test01.cj:82:9:
   | 
82 |         f_B() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'C'
  ==> syscap_test01.cj:83:9:
   | 
83 |         f_C() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'D'
  ==> syscap_test01.cj:84:9:
   | 
84 |         f_D() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'E'
  ==> syscap_test01.cj:85:9:
   | 
85 |         f_E() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'F'
  ==> syscap_test01.cj:86:9:
   | 
86 |         f_F() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'G'
  ==> syscap_test01.cj:87:9:
   | 
87 |         f_G() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

error: inappropriate syscap 'H'
  ==> syscap_test01.cj:88:9:
   | 
88 |         f_H() // error
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A, B, C...

error: inappropriate syscap 'I'
  ==> syscap_test01.cj:89:9:
   | 
89 |         f_I() // error
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: A, B, C...

warning: inappropriate syscap 'C'
  ==> syscap_test01.cj:95:9:
   | 
95 |         f_C() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: B, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'D'
  ==> syscap_test01.cj:96:9:
   | 
96 |         f_D() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: B, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'E'
  ==> syscap_test01.cj:97:9:
   | 
97 |         f_E() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: B, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'F'
  ==> syscap_test01.cj:98:9:
   | 
98 |         f_F() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: B, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'G'
  ==> syscap_test01.cj:99:9:
   | 
99 |         f_G() // warning
   |         ^^^^^ 
   | 
   # note: the following syscaps are supported: B, A
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

error: inappropriate syscap 'H'
   ==> syscap_test01.cj:100:9:
    | 
100 |         f_H() // error
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A, B, C...

error: inappropriate syscap 'I'
   ==> syscap_test01.cj:101:9:
    | 
101 |         f_I() // error
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A, B, C...

warning: inappropriate syscap 'B'
   ==> syscap_test01.cj:104:9:
    | 
104 |         f_B() // warning
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A
    # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'C'
   ==> syscap_test01.cj:105:9:
    | 
105 |         f_C() // warning
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A
    # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'D'
   ==> syscap_test01.cj:106:9:
    | 
106 |         f_D() // warning
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A
    # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'E'
   ==> syscap_test01.cj:107:9:
    | 
107 |         f_E() // warning
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A
    # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'F'
   ==> syscap_test01.cj:108:9:
    | 
108 |         f_F() // warning
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A
    # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: inappropriate syscap 'G'
   ==> syscap_test01.cj:109:9:
    | 
109 |         f_G() // warning
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A
    # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

error: inappropriate syscap 'H'
   ==> syscap_test01.cj:110:9:
    | 
110 |         f_H() // error
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A, B, C...

error: inappropriate syscap 'I'
   ==> syscap_test01.cj:111:9:
    | 
111 |         f_I() // error
    |         ^^^^^ 
    | 
    # note: the following syscaps are supported: A, B, C...

11 errors generated, 11 errors printed.
35 warnings generated, 35 warnings printed.
*/

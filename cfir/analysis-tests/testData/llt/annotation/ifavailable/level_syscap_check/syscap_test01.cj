// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj json

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
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        f_H() // ok
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    })

    @IfAvailable(syscap: "A", {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    })

    @IfAvailable(syscap: "B", {=> // intersectionSet: [A, B], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        f_B() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    })
}

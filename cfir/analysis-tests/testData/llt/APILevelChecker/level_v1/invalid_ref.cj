// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj

import ohos.labels.*



@!APILevel[10]
public func foo(): Unit {
    <!APILEVEL_REF_HIGHER!>goo<!> // error
    <!APILEVEL_REF_HIGHER!>hoo<!> // error
    koo // ok
}

@!APILevel[11]
public func goo(): Unit {
    foo // ok
    <!APILEVEL_REF_HIGHER!>hoo<!> // error
    koo // ok
}

@!APILevel[12]
public func hoo(): Unit {
    goo() // ok
    foo() // ok
    koo() // ok
}


public func koo(): Unit {
    foo() // ok
    goo() // ok
    <!APILEVEL_REF_HIGHER!>hoo()<!> // error
}

main() {}

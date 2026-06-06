// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj mix_macro_dep.cj mix_macro_dep1.cj

package ohos.dep

import <!package_search_error!>ohos.labels<!>.*
import <!package_search_error!>m<!>.*
import <!package_search_error!>anno<!>.*

@!APILevel[since: "21"]
@M
public open class A {
}

@!APILevel[since: "21"]
@CustomAnno
public open class B {
}

@!Hide[isChecked: true]
@M
public open class C {
}

@!Hide[isChecked: true]
@CustomAnno
public open class D {
}

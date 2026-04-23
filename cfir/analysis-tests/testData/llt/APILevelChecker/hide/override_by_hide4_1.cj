// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj hide_use1.cj

package ohos.dep

import ohos.labels.*

public interface I {
    func foo() {}
    func goo() {}
    func koo() {}
}

public class A {}

@!Hide
extend A <: I {
    public func <!HIDE_MISSING_HIDE!>foo<!>() {}
    public func <!HIDE_MISSING_HIDE!>goo<!>() {}
    public func <!HIDE_MISSING_HIDE!>koo<!>() {}
}

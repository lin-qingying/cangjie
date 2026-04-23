// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj call_hide_extend_dep.cj

package ohos.dep

import ohos.labels.*

public class A {}

@!Hide[isChecked: false]
extend A {
    @!Hide
    public func foo() {}
    @!Hide[isChecked: false]
    public func goo() {}
    @<!HIDE_DIFF_PARAM!>!Hide[isChecked: true]<!> // error
    public func hoo() {}
    public func ioo() {}
}

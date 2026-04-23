// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj call_hide_extend_dep.cj

package ohos.dep

import ohos.labels.*

public class A {
    public A(@!Hide[isChecked: true] public var a!: Int64 = 1/* ok */, @<!HIDE_AT_FUNC_PARAM!>!Hide[isChecked: true] b!: Int64<!> = 2/* error */) {}

    public init(@<!HIDE_AT_FUNC_PARAM!>!Hide[isChecked: true] b!: String<!>) { a = 2 }

    public func foo(@<!HIDE_AT_FUNC_PARAM!>!Hide[isChecked: true] c: Int64<!>) {}
}

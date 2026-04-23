// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj hide_use1.cj

package ohos.dep

import ohos.labels.*

@!Hide
public interface I1 {
    func foo() {}
}

@!Hide
public class A1 <: I1 {
    public func foo() {}
}

@!Hide[isChecked: true]
public interface I2 {
    func foo() {}
}

@!Hide[isChecked: true]
public class A2 <: I2 {
    public func foo() {}
}

@!Hide
public interface I3 {
    func foo() {}
}

@!Hide[isChecked: false]
public class A3 <: I3 {
    public func foo() {}
}

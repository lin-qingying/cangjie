// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package ohos.dep

import ohos.labels.*

public class A {
    public func foo() {}
}

@!Hide[isChecked: true]
extend A {
    public func goo() {}
}

extend A {
    @!Hide[isChecked: true]
    public func koo() {}
}

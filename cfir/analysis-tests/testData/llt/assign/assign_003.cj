// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {}

class B<T> {}

class C {
    public var list: B<A> = B<A>()
    public func onInit() {
        let data = [A(), A()]
        this.list = <!TYPE_MISMATCH!>data<!>
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct R1<T> {
    <!CLASS_UNINITIALIZED_FIELD!>public var a: T<!>

    public func foo1(x: T) {
        this.<!CANNOT_MODIFY_VAR!>a<!> = x
    }

    public func foo2(x: T) {
        <!CANNOT_MODIFY_VAR!>a<!> = x
    }
}

main() {
    return 1
}

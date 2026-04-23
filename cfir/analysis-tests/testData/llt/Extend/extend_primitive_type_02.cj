// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

extend Int64 {
    public func add() {
        return this + 1
    }
}

main() {
    1.add
    1.<!UNRESOLVED_REFERENCE!>addd<!>
    0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class d {
    public func test() {}
    public var b = <!USED_BEFORE_INITIALIZATION!>test<!>
}

main() {
    var a = d()
    var b = a.test
    return 0
}

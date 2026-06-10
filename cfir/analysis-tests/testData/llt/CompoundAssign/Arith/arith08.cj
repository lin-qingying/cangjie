// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct Rectangle {
    public var width: Int32 = 1
    public let length: Int32 = 1
    public func set() {
        <!sema_cannot_modify_var!>width<!> += 1
    }
}

main(): Int64 {
    return 0
}

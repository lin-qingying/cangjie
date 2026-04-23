// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Rectangle {
    public let width: Int64
    public let length: Int64

    public init() {
        this.width = 10
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.width<!> = 11
        this.length = 20
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.length<!> = 21
    }
}

main(): Int64 {
    var rec = Rectangle()
    if (rec.width == 11 && rec.length == 21) {
        return 0
    }

    return 1
}

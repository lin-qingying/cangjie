// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct RA {
    public let a: Int64 = 10
    public init(a: Int64) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.a<!> = a
    }
}

main() {
    var ra = RA(5)
    if (ra.a == 5) {
        return 0
    }
    return 1
}

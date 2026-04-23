// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Boo {
    public var x: Int64
    public init(x: Int64) {
        this.x = x
    }
}

class Coo <: Boo {
    public var y: Int64
    public init(x: Int64) {
        try {
            <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>(x)
            this.y = x
        } catch (e: NegativeArraySizeException) {
            throw e
        }
    }
}

main() {}

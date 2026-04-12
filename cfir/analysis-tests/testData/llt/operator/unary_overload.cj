// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Point <: ToString {
    var x: Int64
    var y: Int64
    init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }

    operator func -(): Point {
        return Point(-x, -y)
    }

    public func toString(): String {
        return "Point(${x}, ${y})"
    }
}

main() {
    let p = Point(3, 2)
    let i: ToString = -p
    0
}

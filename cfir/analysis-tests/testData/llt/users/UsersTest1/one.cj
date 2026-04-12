// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt  %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Mike {
    public var dd: Int32 = 1
    protected var b = 3
    public static var c = 4
    public init() {}
    public init(a: Int32) {
        this.dd = a
    }

    public func gxd(): Int64 {
        var tmp: Int64 = 33
        return tmp
    }
}

main(): Int64 {
    var pg: Int64 = 4;
    if (pg == 3) {
        var d: Int64 = 5;
    }

    var XXObject: Mike = Mike();
    return Mike().gxd() - 33;
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var atest: Int32 = 1

public var (a, b) = (1, 2)

class Basec {
    public var atest = atest
    protected var two = 2
    private var seven = 7
    public static var three = 3
    protected static var five = 5
    private static var six = 6
}

main() {
    return 0
}

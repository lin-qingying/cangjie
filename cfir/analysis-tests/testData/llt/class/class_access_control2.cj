<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


var MinTickValue: Int64 = 1

class DateTime {
    private var Ticks: Int64 = MinTickValue
    private func SetTick(tick: Int64) {
        Ticks = tick
    }

    public init() {
        Ticks = MinTickValue
    }

    public static func MaxValue(): DateTime {
        var d = DateTime()
        d.SetTick(MinTickValue)
        <!sema_static_function_cannot_access_non_static_member!>S<!>etTick(MinTickValue)
        var a = <!sema_static_function_cannot_access_non_static_member!>T<!>icks
        return d
    }
}

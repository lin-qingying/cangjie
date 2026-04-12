// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

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
        SetTick(MinTickValue)
        var a = Ticks
        return d
    }
}

/* SCAN
error: 'SetTick' is non-static member, cannot be accessed by static function 'MaxValue'
  ==> class_access_control2.cj:24:9:
   | 
24 |         SetTick(MinTickValue)
   |         ^ 
   | 

error: 'Ticks' is non-static member, cannot be accessed by static function 'MaxValue'
  ==> class_access_control2.cj:25:17:
   | 
25 |         var a = Ticks
   |                 ^ 
   | 

2 errors generated, 2 errors printed.
*/

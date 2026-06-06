<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

let a = true
let b: UInt16 = 5

struct c {
    <!sema_class_uninitialized_field!>p<!>ublic let d: UInt16
    public func e() {
        match {
            case <!sema_mismatched_types!>{=>}<!> => if (true) {
                b
            } else {
                UInt16(2)
            }
            case _ => if (a) {
                d
            } else {
                UInt16(9)
            }
        }
    }
}

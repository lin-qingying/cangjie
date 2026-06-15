<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f<T>(a: T) {}

class G {
    init() {}
    init(a: Int64) {}
}

func func657(): UInt64 {
    var x: Option<UInt64> = Some(1)
    x ?? try {
        UInt64(1)
    } catch (_) {
        [Option<UInt64>.None, Option<UInt64>.<!NOT_MEMBER_OF!>NonZ<!>];
        f(G())
        UInt64(2)
    }
}

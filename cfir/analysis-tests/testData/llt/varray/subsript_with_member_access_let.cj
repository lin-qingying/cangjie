// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    var fdset = FdSetVal()
    fdOne(fdset)
}

@C
public struct FdSetVal {
    var fdBits = VArray<Int64, $6>(repeat: 0)
}

public func fdOne(fdset: FdSetVal): Unit {
    let size: Int64 = fdset.fdBits.size
    for (i in 0..size) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>fdset.fdBits[i]<!> = 1
        println(fdset.fdBits[i])
    }
}

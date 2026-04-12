// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %enableO2 %f -o %output
// EXEC: %run %run_opt %output %run_args

struct R<o, j> {
    let n: (Range<Int16>) = 127..7701
}

main() {
    try {
        func k<o>() {
            let x: R<Int16, Range<Int32>>
            var l: Range<UInt64> = 127..155
        }
        k<Option<Int32>>()
    } catch (e: NoneValueException) {
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output
// (CJNATIVE) RUN-EXEC: %run %run_opt %output %run_args
// (not CJNATIVE) ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {}

class A {}

extend A <: I {}

func get(): ((A, A), Int64) {
    return ((A(), A()), 1)
}

main() {
    var i: (I, I)
    var val = 1
    <!TYPE_MISMATCH!>(i, val) = get()<!>
    0
}

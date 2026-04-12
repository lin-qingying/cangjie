// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

func func_166() {
    try {
        var d = {
            => return 4
        }
        return d()
    } finally {
    }
}

main() {
    var zz = func_166();
    if (zz.toString() != "4") {
        return 1
    }
    return 0;
}

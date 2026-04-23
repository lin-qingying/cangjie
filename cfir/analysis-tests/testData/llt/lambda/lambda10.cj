// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum Result {
    Abort | Continue | Retry
}

func visit(handler: (String) -> Unit) {
    2
}

func visit(handler: (Int64) -> Result) {
    1
}

main() {
    visit {s => return Result.Abort}
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

public class Buff<E> {
    public var myData2: Array<Option<E>> = [Option<E>.None]
    public var c = match (myData2[0]) {
        case Some(x) => "1"
        case _ => "3"
    }
}

main() {
    var a = Buff<Float32>();
    var b = Buff<Int32>();
    0
}

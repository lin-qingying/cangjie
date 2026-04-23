// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

@Annotation[target: [GlobalFunction]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[11]<!>
var (a, b) = (3, "")

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[11]<!>
const c = -1

main() {}

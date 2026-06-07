// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type ran = Range<Int64<!parse_expected_right_delimiter!>
<!>
func test(a: ran) {
    let arr = [1]
    arr[a]
    0
}

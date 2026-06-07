
internal import <!package_search_error!>stdx.encoding.json<!>.*
main() {
    let result1: JsonValue = JsonValue.fromStr(#"{"key1": "value1"}"#)
    let result2 = JsonValue.fromStr(#"{"key2": "value2"}"#)
    println("${result1}" +  "${result2}")
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

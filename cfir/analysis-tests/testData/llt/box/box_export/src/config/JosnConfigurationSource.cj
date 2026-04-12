// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

import std.fs.{exists, File}
import stdx.encoding.json.{JsonValue, JsonObject, JsonArray, JsonKind}
import std.collection.{HashMap, HashSet, collectArray, enumerate}

/*
Json配置源
 */
class JosnConfigurationSource <: ConfigurationSource {
    let _json: String
    let _keys = HashSet<String>()
    let _items = HashMap<String, String>()

    init(json: String) {
        _json = json
    }

    public func build() {
        let jsonValue = JsonValue.fromStr(_json)
        parse(jsonValue)
        return _items
    }

    func parse(jsonValue: JsonValue): Unit {
        match (jsonValue.kind()) {
            case JsonKind.JsArray => parseArray(jsonValue.asArray())
            case JsonKind.JsObject => parseObject(jsonValue.asObject())
            case _ => parseValue(jsonValue)
        }
    }

    func parseObject(jsonObject: JsonObject) {
        for ((key, value) in jsonObject.getFields()) {
            _keys.add(key)
            parse(value)
            _keys.remove(key)
        }
    }

    func parseArray(jsonArray: JsonArray) {
        for ((i, item) in jsonArray.getItems() |> enumerate) {
            _keys.add("${i}")
            parse(item)
            _keys.remove("${i}")
        }
    }

    func parseValue(jsonValue: JsonValue): Unit {
        match (jsonValue.kind()) {
            case JsonKind.JsBool => addValue(jsonValue.asBool().toString())
            case JsonKind.JsNull => addValue(jsonValue.asNull().toString())
            case JsonKind.JsString => addValue(jsonValue.asString().getValue())
            case JsonKind.JsInt => addValue(jsonValue.asInt().getValue().toString())
            case JsonKind.JsFloat => addValue(jsonValue.asFloat().getValue().toString())
            case _ => Exception()
        }
    }

    func addValue(value: String) {
        let key = String.join(_keys |> collectArray, delimiter: ":")
        _items.add(key, value)
    }
}

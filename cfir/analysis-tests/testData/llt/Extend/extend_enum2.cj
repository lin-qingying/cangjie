// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %import-cangjie-stdx %n.cj -o %output
// EXEC: %set_stdx_path %run %run_opt %output

package problem
public import stdx.encoding.json.*

public class JsonTypeMismatch <: Exception {
    public init(expectType: String, actualType: String) {
        super("type mismatch: expect '${expectType}', found '${actualType}'")

    }
    public init(className: String, fieldName: String, message: String) {
        super("'${className}.${fieldName}' ${message}")
    }
}

// 对应的字段不存在的异常
public class JsonFieldNotExist <: Exception {
    public init(className: String, fieldName: String) {
        super("'${className}.${fieldName}' not exist")
    }
}

// 获取 Value 失败的异常。
public class JsonGetValueException <: Exception {
    public init(message: String) {
        super(message)
    }
}

public interface  IJsonAdapter<T> {
    static func fromJson(str: String): T {
        fromJsonValue(JsonValue.fromStr(str))
    }

    static func fromJsonValue(jsonValue: JsonValue): T

    func toJson(): String {
        toJsonValue().toString()
    }

    func toJsonValue(): JsonValue
}

extend Int64 <: IJsonAdapter<Int64> {
    public static func fromJsonValue(jsonValue: JsonValue): Int64 {
        match(jsonValue) {
            case v: JsonInt => return v.getValue()
            case _ => throw JsonTypeMismatch("error", "actualType")
        }
    }

    public func toJsonValue(): JsonValue {
        return JsonInt(this)
    }
}


extend<T> Option<T> <: IJsonAdapter<Option<T>> where T <: IJsonAdapter<T> {
    public static func fromJsonValue(jsonValue: JsonValue): Option<T> {
        if (jsonValue is JsonNull) {
            return None
        } else {
            return T.fromJsonValue(jsonValue)
        }
    }


    public func toJsonValue(): JsonValue {
        if (let Some(v) <- this) {
            v.toJsonValue()
        } else {
            JsonNull()
        }
    }
}
main() : Int64 {
      let value = Option<Int64>.fromJson(Option<Int64>.None.toJson())
      return 0
}

// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：变量声明的类型标注缺失。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_type "expected type name after ':', found '='"。
func missingTypeInVar(): Unit {
    var x: = 1
}

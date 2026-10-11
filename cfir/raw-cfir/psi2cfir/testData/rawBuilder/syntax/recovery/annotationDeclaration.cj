// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：自定义注解声明（官方 1.0.5 / 1.1.3 没有 annotation 声明语法，只有内置注解与属性宏）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_decl "expected declaration, found 'annotation'"。
public annotation MyAnnotation(x: Int64) {
}

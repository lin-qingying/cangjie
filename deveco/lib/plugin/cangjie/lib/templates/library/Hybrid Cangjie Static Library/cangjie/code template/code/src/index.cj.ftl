package ohos_app_cangjie_${moduleName}

internal import ohos.ark_interop.JSModule
internal import ohos.ark_interop.JSContext
internal import ohos.ark_interop.JSCallInfo
internal import ohos.ark_interop.JSValue
internal import cj_res_${moduleName}.app

func testCJ(runtime: JSContext, callInfo: JSCallInfo): JSValue {
    let value = callInfo[0].toString(runtime)
    let result = "Hello <#noparse>${value}</#noparse>"
    runtime.string(result).toJSValue()
}

let EXPORT_MODULE = JSModule.registerModule {
    runtime, exports => exports["testCJ"] = runtime.function(testCJ).toJSValue()
}

// CFunc wrapper 的函数类型参数须应用与 foreign 声明相同的 CType 检查。
type UnitParameter = CFunc<(<!CFUNC_CANNOT_HAVE_UNIT_ARGS!>Unit<!>) -> Unit>
type StringParameter = CFunc<(<!INVALID_CFUNC_PARAMETER_TYPE!>String<!>) -> Unit>
type StringReturn = CFunc<() -> <!INVALID_CFUNC_RETURN_TYPE!>String<!>>
type VArrayReturn = CFunc<() -> <!VARRAY_IN_CFUNC!>VArray<Int64, $2><!>>

open class CFuncCaptureBase {
    func base(): Int32 { 0 }
}

class CFuncCapture <: CFuncCaptureBase {
    var field: Int32 = 1

    prop property: Int32 {
        get() { field }
    }

    func member(): Int32 {
        var local: Int32 = 2
        let localFunc = <!USE_FUNC_CAPTURE_VAR_ALONE!>{ => local }<!>
        let localCapture: CFunc<() -> Int32> = <!USE_FUNC_CAPTURE_VAR_ALONE!>{ => <!CFUNC_CANNOT_CAPTURE_VAR!>local<!> }<!>
        let fieldCapture: CFunc<() -> Int32> = { => <!CFUNC_CANNOT_CAPTURE_THIS!>field<!> }
        let thisFieldCapture: CFunc<() -> Int32> = { => <!CFUNC_CANNOT_CAPTURE_THIS!>this<!>.field }
        let superCapture: CFunc<() -> Int32> = { => <!CFUNC_CANNOT_CAPTURE_THIS!>super<!>.base() }
        let thisFunctionCapture: CFunc<() -> Int32> = { => <!CFUNC_CANNOT_CAPTURE_THIS!>this<!>.member() }
        let localFunctionCapture: CFunc<() -> Int32> = { => <!CFUNC_CANNOT_CAPTURE_VAR!>localFunc<!>() }
        let propertyCapture: CFunc<() -> Int32> = { => <!CFUNC_CANNOT_CAPTURE_THIS!>property<!> }
        localCapture
        fieldCapture
        thisFieldCapture
        superCapture
        thisFunctionCapture
        localFunctionCapture
        propertyCapture
        0
    }
}

main() { 0 }

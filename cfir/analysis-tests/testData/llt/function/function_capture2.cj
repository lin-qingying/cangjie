


func f() {
    var x = 1
    let y = 2
    func g() {
        print("${x}")
    }
    let b = <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
    <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
    g()
    func g2() {
        g()
    }
    let c = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>g<!>2
    func h() {
        print("${y}")
    }
    let d = h
    let e = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> print("${x}")}<!>
    let i = {=> x * x}()
    return <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
}

let x = 4

func f2() {
    var a = x
    let x: Int64
    func f1() {
        <!CAPTURE_BEFORE_INITIALIZATION!>x<!>
    }
    x = 99
    f1()
}

main(): Int64 {
    return 0
}

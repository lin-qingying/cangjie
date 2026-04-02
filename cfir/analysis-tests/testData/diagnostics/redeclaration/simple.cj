
type A = Int64

interface <!CLASSIFIER_REDECLARATION!>abc<!>{
<!CONFLICTING_OVERLOADS!>func ab()<!>:Unit

<!CONFLICTING_OVERLOADS!>func ab()<!>:Unit
    }
class <!CLASSIFIER_REDECLARATION, CONFLICTING_OVERLOADS!>abc<!>{

}
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class <!CLASSIFIER_REDECLARATION, CONFLICTING_OVERLOADS!>abc<!><!>{
  prop  <!REDECLARATION!>a<!>:Int{
        get(){
            1
        }
    }
<!CONFLICTING_OVERLOADS!>func a()<!>{}

    let <!REDECLARATION!>a<!> = 1
        <!CONFLICTING_OVERLOADS!>func a()<!>{

        }
}

<!CONFLICTING_OVERLOADS!>func abc()<!>{}
<!CONFLICTING_OVERLOADS!>func abc()<!>{}


let <!REDECLARATION!>abc<!>  = 1

let (<!REDECLARATION!>abc<!>,<!REDECLARATION!>abc<!>) = (1,2)


func abc1(a:Int){

}
func abc1(a: Int, b:Int){

}

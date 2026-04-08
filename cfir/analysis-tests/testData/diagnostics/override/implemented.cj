

interface A {
    func a(): Unit
    func c(): Unit
    func d(): Unit
    func e(): Unit
    prop f:Int
    prop h:Int
    static func g(): Unit
    static func i(): Unit
}
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class  B<!> <: A{
    <!NOTHING_TO_OVERRIDE!>override<!>  func b(){

    } //上层没有这个方法，无法重写

    override <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>private<!> func c(): Unit {

    }//private和上层的public不兼容
       override  func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>d<!>(): Unit {

       }//internal和上层的public不兼容
         override public  func e(): Unit {

                }



   public override prop f:Int{
           get() {
               return 0
           }
       }
          override prop <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>h<!>:Int{
           get() {
               return 0
           }
       }
       redef static func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>g<!>(): Unit {

       }
        public redef static func i(): Unit {

       }

       <!REDEF_INSTANCE_ERROR!>redef<!>  func j(): Unit {

           }
         <!NOTHING_TO_OVERRIDE!>redef<!>    static func k(): Unit {

           }
}

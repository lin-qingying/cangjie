// FILE: a.cj
package a

public interface FunctionRequirement {
    func function(): Int64
}
public interface PropertyRequirement {
    prop value: Int64
}
public open class Base {}

// FILE: b.cj
package b

import a.Base
public interface ExportedMembers {
    func function(): Int64
    prop value: Int64
}
extend Base <: ExportedMembers {
    public func function(): Int64 { 1 }
    public prop value: Int64 { get() { 2 } }
}
public interface Unrelated {}

// FILE: absent.cj
package absent

import a.*
import <!UNUSED_IMPORT!>b.Unrelated<!>
public class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MissingFunction<!> <: Base & FunctionRequirement {}
public class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MissingProperty<!> <: Base & PropertyRequirement {}

// FILE: present.cj
package present

import a.*
import <!UNUSED_IMPORT!>b.ExportedMembers<!>
public class CompleteFunction <: Base & FunctionRequirement {}
public class CompleteProperty <: Base & PropertyRequirement {}

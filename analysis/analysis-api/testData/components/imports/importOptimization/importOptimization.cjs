// MAIN_FILE_NAME: importOptimization.cjs
// FILE: helpers.cjs
package sample.helpers.script

class User {
}

func makeUser(): User {
    return User()
}

func unusedThing(): Int64 {
    return 2
}

// FILE: importOptimization.cjs
// EXPECTED_RETAINED_IMPORT: sample.helpers.script.makeUser
// EXPECTED_DUPLICATE_IMPORT: sample.helpers.script.makeUser
// EXPECTED_UNUSED_IMPORT: sample.helpers.script.unusedThing
package sample.imports.script

import sample.helpers.script.makeUser
import sample.helpers.script.makeUser
import sample.helpers.script.unusedThing

func consume(): sample.helpers.script.User {
    return makeUser()
}

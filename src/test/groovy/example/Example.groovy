package example

import spock.lang.Specification
import spock.lang.Unroll

class Example extends Specification {
    def "This test is passing"() {
        when: "Setting up the number"
        def one = 1

        then: "Doing the check"
        1 == one
    }

    def "This test is failing"() {
        when: "Setting up the number"
        def one = 1

        then: "Doing the check"
        2 == one
    }

    @Unroll("This test is data driven. Using number #number")
    def "This test is data driven"() {
        when: "Setting up the number"
        number += 1

        then: "Doing the check"
        2 == number
        where: number << [1,2,3]
    }
}

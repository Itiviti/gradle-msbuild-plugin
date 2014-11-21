package com.ullink

class EvalTest extends spock.lang.Specification {
    def "check patterns evaluations"() {
        expect:
        new ProjectFileParser().isConditionTrue(expr)

        where:
        expr << [
                'true == true',
                'false == false',
                "'11.0' > '10.0'",
        ]
    }
}

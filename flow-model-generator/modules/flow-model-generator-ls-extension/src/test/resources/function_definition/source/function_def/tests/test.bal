import ballerina/test;

@test:Config {
    groups: ["group1"]
}
function test1() {
    test:assertTrue(true, msg = "test1 failed");
}

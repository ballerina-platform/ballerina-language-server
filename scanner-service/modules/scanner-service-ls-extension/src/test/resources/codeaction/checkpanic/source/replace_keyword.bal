import ballerina/io;

public function main() {
    int|error result = checkpanic getResult();
    io:println(result);
}

function getResult() returns int|error {
    return 1;
}

import ballerina/log;

public type Person record {
    string name;
    int age;
    [int, string, float] data;
};

public function main() returns error? {
    do {
        // [int, string, float] data = [1, "Alice", 99.5];
        // [int, string, float] result = [];
        // int[] numbers = [10, 20, 30];
        // Person person = {name: "Alice" + data[1], age: 30};
        // Person updatedPErson = {age: numbers[1]};

        [int, string, float] data = [1, "Alice", 99.5];
        Person person = {age: data[0]};
    } on fail error e {
        log:printError("Error occurred", 'error = e);
        return e;
    }
}
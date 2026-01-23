import ballerina/log;

type TupleType [int, string, float];

public type Person record {
    string name;
    int age;
    TupleType data;
};

public function main() returns error? {
    do {
        [int, string, float] data = [1, "Alice", 99.5];
        [int, string, float] result = []; //Case 01
        Person person1 = {name: "Bob", age: 30, data: data};
        Person person2 = {data: [result[0], "", 0.0]}; //Case 02
    } on fail error e {
        log:printError("Error occurred", 'error = e);
        return e;
    }
}

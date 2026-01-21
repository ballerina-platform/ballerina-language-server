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
        [int, string, float] result = [];
        Person person1 = {name: "Bob", age: 30, data: data};
        Person person2 = {age: data[0], name: person1.data[1]}; //Case 02
    } on fail error e {
        log:printError("Error occurred", 'error = e);
        return e;
    }
}
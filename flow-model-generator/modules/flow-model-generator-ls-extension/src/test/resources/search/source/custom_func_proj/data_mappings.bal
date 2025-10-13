type Person record {
    string name;
    int age;
};

type Employee record {
    string fullName;
    int age;
};

// This is a data mapping function (expression-bodied)
function mapPersonToEmployee(Person person) returns Employee => {
    fullName: person.name,
    age: person.age
};

// This is a custom function (regular function body), not a data mapping function
function customHelper(string input) returns string {
    return "Processed: " + input;
}

// This is another custom function (regular function body)
isolated function validateAge(int age) returns boolean {
    return age >= 18;
}

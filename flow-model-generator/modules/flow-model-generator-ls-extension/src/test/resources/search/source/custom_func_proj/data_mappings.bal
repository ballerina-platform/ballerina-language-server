import ballerina/ai;

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

// This is a natural expression function - should be skipped entirely
function generateGreeting(string name) returns string|error => natural (check ai:getDefaultModelProvider()) {
    **What to do**
    Generate a personalized greeting for ${name}
    
    **Output**
    string - A friendly greeting message
};

import ballerina/graphql;

service class Profile {
    private final string name;
    private final int age;

    function init(string name, int age) {
        self.name = name;
        self.age = age;
    }

    resource function get name() returns string {
        return self.name;
    }

    resource function get age() returns int {
        return self.age;
    }

    resource function get isAdult() returns boolean {
        return self.age > 21;
    }
}

service class Teacher {
    resource function get name() returns string {
        return "Ballerina";
    }
}

service /graphql on new graphql:Listener(9090) {

    resource function get profile() returns Profile {
        return new ("Walter White", 51);
    }
}

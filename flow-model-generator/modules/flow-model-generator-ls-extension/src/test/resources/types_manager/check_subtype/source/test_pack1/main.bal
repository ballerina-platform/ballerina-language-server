import ballerina/graphql;

service class Teacher {
    resource function get name() returns string {
        return "Walter";
    }
}

service class Student {
    resource function get name() returns string {
        return "Jessie";
    }
}

type Profile Student|Teacher;

service /graphql on new graphql:Listener(9090) {

    resource function get profile() returns Profile {
        return new Teacher();
    }
}

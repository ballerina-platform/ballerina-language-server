import ballerina/graphql;

distinct service class Teacher {
    *graphql:Service;

    resource function get name() returns string {
        return "Walter";
    }
}

distinct service class Student {
    *Teacher;
    resource function get name() returns string {
        return "Jessie";
    }
}

service /api on new graphql:Listener(9091) {

    resource function get profile() returns string? {
        return;
    }
}

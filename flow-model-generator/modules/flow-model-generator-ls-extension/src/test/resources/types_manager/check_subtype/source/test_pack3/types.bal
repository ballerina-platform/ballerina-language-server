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

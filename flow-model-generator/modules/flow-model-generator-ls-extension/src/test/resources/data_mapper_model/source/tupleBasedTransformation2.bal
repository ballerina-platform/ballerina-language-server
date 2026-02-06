type Student record {
    string id;
    string name;
};

type DetailedPerson record {
    string id;
    string name;
};

type CourseDetails record {
    string courseId;
    string courseName;
    decimal credits;
    string grade;
};

type DetailedStudent record {
    Course[] courses;
};

type Course record {
    string id;
    string name;
    float credits;
};

// Case 01
function transform1([int, string, Course] input) returns [int, string, Course] => [0, "", {id: input[2].id}];

type Input record {
    string id;
    string name;
    string email;
};

type Output record {
    string id;
    string name;
    string email;
};

function transformToJson(Input payload) returns json => {
    id: payload.id,
    name: payload.name,
    email: payload.email,
    address: {
        line1 : payload.id,
        line2: foo(payload.id)
    }
};

function foo(string input) returns string {
    return input;
}

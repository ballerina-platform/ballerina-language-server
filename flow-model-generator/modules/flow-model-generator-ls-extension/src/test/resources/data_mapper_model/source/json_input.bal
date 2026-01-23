json inputData = {
    "id": "123",
    "name": "John Doe",
    "email": "john@example.com"
};

type Output record {
    string id;
    string name;
    string email;
};

function transformFromJson() returns Output => {
    id: inputData.id,
    name: inputData.name,
    email: inputData.email
};

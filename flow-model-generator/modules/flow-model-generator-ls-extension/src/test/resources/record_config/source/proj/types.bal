
type Address record {|
    string street;
    string city;
    string country;
|};

type EmployeeLog record {|
    string timestamp;
    string action;
    string userId;
    string description;
|};

type Employee record {|
    *Address;
    string employeeId;
    string department;
    decimal salary;
    EmployeeLog...;
|};

# Immutable server configuration used to connect to a remote endpoint.
# Credentials are shared across callers and must not be mutated.
type SpecializedConfig readonly & ServerConfig;

# Immutable server configuration used to connect to a remote endpoint.
# Credentials are shared across callers and must not be mutated.
type ServerConfig readonly & record {|
    string host;
    int port;
    string apiKey;
|};

import ballerina/http;
import ballerina/io;

listener http:Listener httpDefaultListener = http:getDefaultListener();

service /foo on httpDefaultListener {
    resource function post foo(@http:Payload PayloadType payload) returns error|json {
        do {
            io:println(payload, payload);
        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }

}

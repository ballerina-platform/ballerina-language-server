import ballerina/os;

public function getHomeDir() returns string? {
    return os:getEnv("HOME");
}

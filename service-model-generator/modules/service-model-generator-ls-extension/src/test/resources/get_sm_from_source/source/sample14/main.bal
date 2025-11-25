import ballerinax/gcloud.pubsub;

listener pubsub:Listener pubsubListener = new ("project1", auth = {path: "/Users/radith/Desktop/auth.json"});

@pubsub:ServiceConfig {
    subscription: "sub1"
}
service pubsub:Service on pubsubListener {
    remote function onMessage(PubSubMessage message, pubsub:Caller caller) returns error? {
        do {
        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }

}

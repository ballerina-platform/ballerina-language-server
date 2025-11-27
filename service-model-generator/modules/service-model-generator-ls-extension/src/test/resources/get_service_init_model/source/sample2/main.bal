import ballerinax/rabbitmq;
import ballerinax/solace;
import ballerinax/gcloud.pubsub;

listener rabbitmq:Listener orderListener = new (rabbitmq:DEFAULT_HOST, 5671);
listener rabbitmq:Listener deliveryListener = new (rabbitmq:DEFAULT_HOST, 5671);
listener solace:Listener solaceListener = new ("smf://localhost:55554", messageVpn = "default");
listener pubsub:Listener pubsubListener = new ("project1", auth = {path: "path/to/auth.json"});

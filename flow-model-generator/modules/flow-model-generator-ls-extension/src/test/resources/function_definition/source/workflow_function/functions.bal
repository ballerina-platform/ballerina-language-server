import ballerina/workflow;

type OrderInput record {
    string orderId;
};

# Process an order workflow
@workflow:Workflow
function orderWorkflow(workflow:Context context, OrderInput input) returns error? {
    // body
}

@workflow:Workflow
function workflowWithNoInput() returns error? {
    // body
}

public type Input record {|
    string id;
    int[] quantities;
    string[] lineItems;
|};

public type Output record {|
    string orderId;
    string[] items;
    string status;
|};

function transform(Input input) returns Output =>
    let decimal subtotal = calculateSum(input.quantities),
        decimal delivery = subtotal * 0.1d,
        decimal total = subtotal + delivery,
        string status = total > 500d ? "Discount Applied" : "Standard Rate"
    in
    {
        orderId: input.id,
        items: input.lineItems,
        status: status
    };


public function calculateSum(int[] quantities) returns decimal {
    decimal sum = 0d;
    foreach int qty in quantities {
        sum += <decimal>qty;
    }
    return sum;
}

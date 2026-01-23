type Baz record {|
    int x;
    int y;
|};

type Bar record {|
    int a;
    int b;
|};

public function main() returns error? {
    do {
        json result = {"bars": 123};
        result = {"hello" : 12};
        Baz b = {x: 10, y: 20};
    } on fail error e {
        return e;
    }
}

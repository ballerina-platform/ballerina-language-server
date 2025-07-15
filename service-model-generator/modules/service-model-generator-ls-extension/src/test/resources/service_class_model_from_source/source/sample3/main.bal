public service class Book {
    private final string id;
    private final string name;
    private Author[] authors = [];

    function init(string name) returns error? {
        do {
            self.id = "";
            self.name = name;
        } on fail error err {
            // hanlde error
        }
    }

    @graphql:ResourceConfig {
        cacheConfig: {
            enabled: true
        }
    }
    resource function get author() returns string|error? {
        do {
            Author[] result = from Author author in self.authors
                where author.id == id
                limit 1
                select author;
            if result.length() > 0 {
                return result[0].name;
            }
            check error("Author not found!");
        } on fail error err {
            // hanlde error
            return err;
        }
    }

    @graphql:ResourceConfig {
        cacheConfig: {
            maxAge: 60
        }
    }
    resource function get address(Author author) {
        do {
            self.authors.push(author);
        } on fail error err {
            // hanlde error
        }
    }
}

type Author record {|
    int id;
    string name;
|};

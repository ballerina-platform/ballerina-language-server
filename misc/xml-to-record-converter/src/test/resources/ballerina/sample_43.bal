type Book record {
    string title;
    string author;
    int year;
    int isbn;
};

type Library record {
    Book[] book;
};

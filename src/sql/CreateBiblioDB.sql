# JavaDB Database setup

DROP TABLE loans;
DROP TABLE books;
DROP TABLE members;
DROP TABLE users;
DROP SCHEMA admin RESTRICT;

CREATE SCHEMA admin;

CREATE TABLE admin.users(
    iduser INTEGER NOT NULL,
    name VARCHAR(20),
    PRIMARY KEY (iduser)
);

CREATE TABLE admin.books (
    idbook INTEGER NOT NULL,
    title VARCHAR(120),
    author VARCHAR(60),
    lent BOOLEAN,
    PRIMARY KEY (idbook)
);

CREATE TABLE admin.members (
    idmember INTEGER NOT NULL,
    name VARCHAR(20),
    surname VARCHAR(40),
    PRIMARY KEY (idmember)
);

CREATE TABLE admin.loans (
    idloan INTEGER NOT NULL,
    idmember INTEGER NOT NULL,
    idbook INTEGER NOT NULL,
    dateloan DATE,
    PRIMARY KEY (idloan),
    FOREIGN KEY (idmember) REFERENCES members(idmember),
    FOREIGN KEY (idbook) REFERENCES books(idbook)
);

# JavaDB Database setup
# JDBC URL: jdbc:derby://localhost:1527/biblioteca
# Autor: Juan García Sánchez
# User: root
# Password: root

DROP TABLE prestamos;
DROP TABLE libros;
DROP TABLE socios;

CREATE TABLE libros (
    idlib INTEGER NOT NULL,
    titulo VARCHAR(120),
    autor VARCHAR(60),
    prestado BOOLEAN,
    PRIMARY KEY (idlib)
);

CREATE TABLE socios (
    idsoc INTEGER NOT NULL,
    nombre VARCHAR(20),
    apellidos VARCHAR(40),
    PRIMARY KEY (idsoc)
);

CREATE TABLE prestamos (
    idpres INTEGER NOT NULL,
    idsoc INTEGER NOT NULL,
    idlib INTEGER NOT NULL,
    fechapres DATE,
    PRIMARY KEY (idpres),
    FOREIGN KEY (idsoc) REFERENCES socios(idsoc),
    FOREIGN KEY (idlib) REFERENCES libros(idlib)
);

# JavaDB Database setup

DROP TABLE prestamos;
DROP TABLE libros;
DROP TABLE socios;
DROP TABLE usuarios;
DROP SCHEMA admin RESTRICT;

CREATE SCHEMA admin;

CREATE TABLE admin.usuarios(
    iduser INTEGER NOT NULL,
    nombre VARCHAR(20),
    PRIMARY KEY (iduser)
);

CREATE TABLE admin.libros (
    idlib INTEGER NOT NULL,
    titulo VARCHAR(120),
    autor VARCHAR(60),
    prestado BOOLEAN,
    PRIMARY KEY (idlib)
);

CREATE TABLE admin.socios (
    idsoc INTEGER NOT NULL,
    nombre VARCHAR(20),
    apellidos VARCHAR(40),
    PRIMARY KEY (idsoc)
);

CREATE TABLE admin.prestamos (
    idpres INTEGER NOT NULL,
    idsoc INTEGER NOT NULL,
    idlib INTEGER NOT NULL,
    fechapres DATE,
    PRIMARY KEY (idpres),
    FOREIGN KEY (idsoc) REFERENCES socios(idsoc),
    FOREIGN KEY (idlib) REFERENCES libros(idlib)
);
